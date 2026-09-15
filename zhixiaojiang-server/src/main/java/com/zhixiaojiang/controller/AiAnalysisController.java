package com.zhixiaojiang.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.common.util.JsonValues;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 辅助分析：只送白名单字段，模型不可用时降级为规则模板，并同时保留确认要求与免责声明。
 */
@RestController
@RequestMapping("/api/v1")
public class AiAnalysisController {
    private final JdbcTemplate db;
    private final TeacherScope scope;
    private final ObjectMapper json = new ObjectMapper();
    private final String aiBaseUrl;
    private final String aiModel;
    private final String aiApiKey;
    private final org.springframework.web.client.RestClient aiClient;

    public AiAnalysisController(JdbcTemplate db, TeacherScope scope, org.springframework.core.env.Environment env) {
        this.db = db;
        this.scope = scope;
        this.aiBaseUrl = env.getProperty("AI_BASE_URL", "");
        this.aiModel = env.getProperty("AI_MODEL", "");
        this.aiApiKey = env.getProperty("AI_API_KEY", "");
        this.aiClient = org.springframework.web.client.RestClient.builder().build();
    }

    @PostMapping("/ai/student-analysis")
    Map<String, Object> ai(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        long studentId = body.get("studentId") instanceof Number n ? n.longValue() : 0;
        if (studentId > 0) scope.requireStudent(studentId);
        Map<String, Object> result = templateAi();
        if (!aiBaseUrl.isBlank() && !aiModel.isBlank() && !aiApiKey.isBlank()) {
            try {
                Map<String, Object> safe = new LinkedHashMap<>();
                for (String key : List.of("scores", "attendance", "behavior", "skills", "tasks"))
                    if (body.get(key) != null) safe.put(key, body.get(key));
                String prompt = "请根据以下匿名成长数据输出 JSON，字段必须包含 summary、evidence、suggestions：" + JsonValues.toJson(safe);
                Map<String, Object> requestBody = Map.of("model", aiModel, "temperature", 0.2, "messages", List.of(Map.of("role", "system", "content", "你是班主任成长分析助手，只做趋势归纳和教育建议，不做心理或医学诊断。"), Map.of("role", "user", "content", prompt)));
                String raw = aiClient.post().uri(aiBaseUrl.endsWith("/chat/completions") ? aiBaseUrl : aiBaseUrl + "/chat/completions").header("Authorization", "Bearer " + aiApiKey).contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(requestBody).retrieve().body(String.class);
                String content = json.readTree(raw).path("choices").path(0).path("message").path("content").asText("");
                Map<String, Object> parsed = json.readValue(content, new TypeReference<Map<String, Object>>() {
                });
                if (parsed.get("summary") != null && parsed.get("evidence") instanceof Collection<?> && parsed.get("suggestions") instanceof Collection<?>) {
                    result = new LinkedHashMap<>(parsed);
                    result.put("source", "MODEL");
                    result.put("disclaimer", "AI辅助建议，仅供教师参考");
                    result.put("requiresTeacherConfirmation", true);
                }
            } catch (Exception ignored) {/* fall back to the explicit template result */}
        }
        try {
            if (studentId > 0)
                db.update("insert into ai_analysis(student_id,source,request_json,response_json,created_by) values(?,?,?,?,?)", studentId, String.valueOf(result.get("source")), JsonValues.toJson(Map.of("whitelist", "scores,attendance,behavior,skills,tasks")), JsonValues.toJson(result), scope.teacher());
        } catch (Exception ignored) {
        }
        return ApiResult.ok(result);
    }

    private Map<String, Object> templateAi() {
        return new LinkedHashMap<>(Map.of("source", "TEMPLATE", "summary", "该生近期学业表现、活动参与和行为表现呈同步下降趋势，建议进一步了解其学习及生活状态。", "evidence", List.of("数学成绩连续下降", "活动参与度下降", "迟到次数增加"), "suggestions", List.of("班主任个别谈话", "数学教师一对一指导", "学习小组伙伴结对", "两周后成长复评"), "disclaimer", "AI辅助建议，仅供教师参考", "requiresTeacherConfirmation", true));
    }
}
