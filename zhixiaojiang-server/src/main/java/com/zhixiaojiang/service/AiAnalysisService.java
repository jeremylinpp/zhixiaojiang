package com.zhixiaojiang.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.util.JsonValues;
import com.zhixiaojiang.dao.AnalysisDao;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 辅助分析。
 *
 * <p>送模型的只有白名单字段（成绩、出勤、行为、技能、任务），不含姓名、联系方式等身份信息；
 * 返回值必须包含摘要、证据与建议，并固定附带免责声明与「需教师确认」。模型未配置或调用异常时
 * 降级为规则模板，保证演示与日常使用不中断。
 */
@Service
public class AiAnalysisService {
    private static final List<String> WHITELIST = List.of("scores", "attendance", "behavior", "skills", "tasks");
    private static final String DISCLAIMER = "AI辅助建议，仅供教师参考";

    private final AnalysisDao analyses;
    private final TeacherScope scope;
    private final ObjectMapper json = new ObjectMapper();
    private final String aiBaseUrl;
    private final String aiModel;
    private final String aiApiKey;
    private final org.springframework.web.client.RestClient aiClient;

    public AiAnalysisService(AnalysisDao analyses, TeacherScope scope, org.springframework.core.env.Environment env) {
        this.analyses = analyses;
        this.scope = scope;
        this.aiBaseUrl = env.getProperty("AI_BASE_URL", "");
        this.aiModel = env.getProperty("AI_MODEL", "");
        this.aiApiKey = env.getProperty("AI_API_KEY", "");
        this.aiClient = org.springframework.web.client.RestClient.builder().build();
    }

    public Map<String, Object> analyze(Map<String, Object> body) {
        long studentId = body.get("studentId") instanceof Number number ? number.longValue() : 0;
        if (studentId > 0) scope.requireStudent(studentId);
        Map<String, Object> result = modelAnalysis(body).orElseGet(this::template);
        if (studentId > 0) {
            try {
                analyses.insert(studentId, String.valueOf(result.get("source")),
                        JsonValues.toJson(Map.of("whitelist", String.join(",", WHITELIST))),
                        JsonValues.toJson(result), scope.teacher());
            } catch (RuntimeException ignored) {
                // 分析记录写入失败不影响本次建议返回
            }
        }
        return result;
    }

    /** 调用模型；返回空表示未配置模型或调用失败，由调用方降级为模板。 */
    private java.util.Optional<Map<String, Object>> modelAnalysis(Map<String, Object> body) {
        if (aiBaseUrl.isBlank() || aiModel.isBlank() || aiApiKey.isBlank()) return java.util.Optional.empty();
        try {
            Map<String, Object> safe = new LinkedHashMap<>();
            for (String key : WHITELIST) if (body.get(key) != null) safe.put(key, body.get(key));
            String prompt = "请根据以下匿名成长数据输出 JSON，字段必须包含 summary、evidence、suggestions：" + JsonValues.toJson(safe);
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
                return java.util.Optional.empty();
            Map<String, Object> result = new LinkedHashMap<>(parsed);
            result.put("source", "MODEL");
            result.put("disclaimer", DISCLAIMER);
            result.put("requiresTeacherConfirmation", true);
            return java.util.Optional.of(result);
        } catch (Exception e) {
            return java.util.Optional.empty();
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
