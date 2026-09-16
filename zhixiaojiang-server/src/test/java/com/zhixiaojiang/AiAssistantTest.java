package com.zhixiaojiang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 智小匠助手：能力状态取真实数据，AI 分析的上下文由服务端组装且不含身份信息。
 */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class AiAssistantTest {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    Cookie session, csrfCookie;
    String csrf;

    private void signIn() throws Exception {
        session = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"teacher\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("ZJ_SESSION");
        var response = http.perform(get("/api/v1/auth/csrf").cookie(session)).andExpect(status().isOk()).andReturn().getResponse();
        csrfCookie = response.getCookie("XSRF-TOKEN");
        csrf = json.readTree(response.getContentAsByteArray()).path("data").path("token").asText();
    }

    private JsonNode data(MockHttpServletRequestBuilder request) throws Exception {
        return json.readTree(http.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }

    @Test
    void statusReportsRealVolumeAndAiMode() throws Exception {
        signIn();
        var status = data(get("/api/v1/assistant/status").cookie(session));

        var ai = status.path("ai");  // data 直接就是能力状态与数据量
        assertFalse(ai.path("configured").asBoolean(), "demo 环境未配置模型");
        assertTrue(ai.path("model").isNull());
        assertEquals(5, ai.path("whitelist").size());
        assertTrue(ai.path("disclaimer").asText().contains("AI辅助建议"));
        assertTrue(ai.path("requiresTeacherConfirmation").asBoolean());

        var volume = status.path("data");
        assertEquals(42, volume.path("students").asInt());
        assertTrue(volume.path("growthRecords").asInt() >= 4, "演示数据已录入成长记录");
        assertTrue(volume.path("scores").asInt() >= 4);
        assertTrue(volume.has("skills") && volume.has("openWarnings"));
        assertTrue(volume.has("lastRuleAnalysisAt"), "缺少最近规则筛查时间字段");

        assertEquals(4, status.path("boundaries").size());
    }

    @Test
    void analysisContextIsAssembledServerSideAndStaysAnonymous() throws Exception {
        signIn();
        var result = data(post("/api/v1/ai/student-analysis").cookie(session, csrfCookie).header("X-CSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":1,\"name\":\"伪造姓名\",\"summary\":\"伪造结论\"}"));

        assertEquals("TEMPLATE", result.path("source").asText(), "未配置模型时降级为规则模板");
        assertTrue(result.path("evidence").isArray() && result.path("suggestions").isArray());
        assertEquals("AI辅助建议，仅供教师参考", result.path("disclaimer").asText());
        assertTrue(result.path("requiresTeacherConfirmation").asBoolean());
        assertNotEquals("伪造结论", result.path("summary").asText(), "客户端提交的结论不得被采用");

        Long latest = db.queryForObject("select max(id) from ai_analysis where student_id=1", Long.class);
        String request = db.queryForObject("select request_json from ai_analysis where id=?", String.class, latest);
        assertTrue(request.contains("scores") && request.contains("attendance") && request.contains("tasks"), "上下文应含白名单字段：" + request);
        assertFalse(request.contains("小智"), "送模型的数据不得包含学生姓名：" + request);
        assertFalse(request.contains("伪造姓名"), "客户端提交的内容不得进入上下文：" + request);
    }

    @Test
    void analysisRequiresStudentId() throws Exception {
        signIn();
        http.perform(post("/api/v1/ai/student-analysis").cookie(session, csrfCookie).header("X-CSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }
}
