package com.zhixiaojiang.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.util.JsonValues;
import com.zhixiaojiang.dao.AnalysisContextMapper;
import com.zhixiaojiang.dao.AnalysisMapper;
import com.zhixiaojiang.model.po.AiAnalysis;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI 辅助分析。
 *
 * <p>送模型的上下文由服务端按学生 id 组装，只包含白名单字段（成绩、出勤、行为、技能、任务），
 * 不含姓名、学号、联系方式等身份信息；返回内容必须包含摘要、证据与建议，并固定附带免责声明
 * 与「需教师确认」。模型未配置或调用异常时降级为规则模板，保证演示与日常使用不中断。
 */
@Service
public class AiAnalysisService {
    private static final List<String> WHITELIST = List.of("scores", "attendance", "behavior", "skills", "tasks");
    private static final String DISCLAIMER = "AI辅助建议，仅供教师参考";
    /** 出勤只统计最近 30 天，避免长周期数据淹没近期变化。 */
    private static final int ATTENDANCE_WINDOW_DAYS = 30;

    private final AnalysisMapper analyses;
    private final AnalysisContextMapper context;
    private final TeacherScope scope;
    private final ObjectMapper json = new ObjectMapper();
    private final String aiBaseUrl;
    private final String aiModel;
    private final String aiApiKey;
    private final org.springframework.web.client.RestClient aiClient;

    public AiAnalysisService(AnalysisMapper analyses, AnalysisContextMapper context, TeacherScope scope, org.springframework.core.env.Environment env) {
        this.analyses = analyses;
        this.context = context;
        this.scope = scope;
        this.aiBaseUrl = env.getProperty("AI_BASE_URL", "");
        this.aiModel = env.getProperty("AI_MODEL", "");
        this.aiApiKey = env.getProperty("AI_API_KEY", "");
        this.aiClient = org.springframework.web.client.RestClient.builder().build();
    }

    /** 助手页展示的 AI 能力状态：是否接入模型、使用哪个模型、白名单字段与免责声明。 */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("configured", modelConfigured());
        status.put("model", modelConfigured() ? aiModel : null);
        status.put("whitelist", WHITELIST);
        status.put("disclaimer", DISCLAIMER);
        status.put("requiresTeacherConfirmation", true);
        return status;
    }

    /** 生成辅助分析：服务端组装上下文，模型不可用时返回规则模板。 */
    public Map<String, Object> analyze(long studentId) {
        scope.requireStudent(studentId);
        Map<String, Object> sent = contextOf(studentId);
        Map<String, Object> result = modelAnalysis(sent).orElseGet(this::template);
        try {
            AiAnalysis record = new AiAnalysis();
            record.setStudentId(studentId);
            record.setSource(String.valueOf(result.get("source")));
            record.setRequestJson(JsonValues.toJson(sent));
            record.setResponseJson(JsonValues.toJson(result));
            record.setCreatedBy(scope.teacher());
            analyses.insert(record);
        } catch (RuntimeException ignored) {
            // 分析记录写入失败不影响本次建议返回
        }
        return result;
    }

    /** 按学生组装白名单上下文：只取事实类字段，不含身份信息。 */
    private Map<String, Object> contextOf(long studentId) {
        Map<String, Object> sent = new LinkedHashMap<>();
        sent.put("scores", context.scores(studentId));
        sent.put("attendance", context.attendance(studentId, LocalDate.now().minusDays(ATTENDANCE_WINDOW_DAYS)));
        sent.put("behavior", context.behavior(studentId));
        sent.put("skills", context.skills(studentId));
        sent.put("tasks", context.tasks(studentId));
        return sent;
    }

    private boolean modelConfigured() {
        return !aiBaseUrl.isBlank() && !aiModel.isBlank() && !aiApiKey.isBlank();
    }

    /** 调用模型；返回空表示未配置模型或调用失败，由调用方降级为模板。 */
    private Optional<Map<String, Object>> modelAnalysis(Map<String, Object> sent) {
        if (!modelConfigured()) return Optional.empty();
        try {
            String prompt = "请根据以下匿名成长数据输出 JSON，字段必须包含 summary、evidence、suggestions：" + JsonValues.toJson(sent);
            Map<String, Object> request = Map.of("model", aiModel, "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content", "你是班主任成长分析助手，只做趋势归纳和教育建议，不做心理或医学诊断。"),
                            Map.of("role", "user", "content", prompt)));
            String raw = aiClient.post()
                    .uri(aiBaseUrl.endsWith("/chat/completions") ? aiBaseUrl : aiBaseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + aiApiKey)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(request).retrieve().body(String.class);
            String content = json.readTree(raw).path("choices").path(0).path("message").path("content").asText("");
            Map<String, Object> parsed = json.readValue(content, new TypeReference<Map<String, Object>>() {
            });
            if (parsed.get("summary") == null || !(parsed.get("evidence") instanceof Collection<?>) || !(parsed.get("suggestions") instanceof Collection<?>))
                return Optional.empty();
            Map<String, Object> result = new LinkedHashMap<>(parsed);
            result.put("source", "MODEL");
            result.put("disclaimer", DISCLAIMER);
            result.put("requiresTeacherConfirmation", true);
            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Map<String, Object> template() {
        return new LinkedHashMap<>(Map.of("source", "TEMPLATE",
                "summary", "该生近期学业表现、活动参与和行为表现呈同步下降趋势，建议进一步了解其学习及生活状态。",
                "evidence", List.of("数学成绩连续下降", "活动参与度下降", "迟到次数增加"),
                "suggestions", List.of("班主任个别谈话", "数学教师一对一指导", "学习小组伙伴结对", "两周后成长复评"),
                "disclaimer", DISCLAIMER,
                "requiresTeacherConfirmation", true));
    }
}
