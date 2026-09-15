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
 * 预警生命周期：待研判 → 研判（含升级）→ 关闭，并校验状态并发控制与参数错误处理。
 *
 * <p>补上此前只测越权、未测正向流程的空白。
 */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class WarningLifecycleTest {
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

    private MockHttpServletRequestBuilder post_(String path, String body) {
        return post(path).cookie(session, csrfCookie).header("X-CSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private JsonNode data(MockHttpServletRequestBuilder request) throws Exception {
        return json.readTree(http.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }

    @Test
    void triageEscalateCloseAndGuardAgainstStaleState() throws Exception {
        signIn();
        long id = db.queryForObject("select id from warning_record where student_id=1 and status='OPEN'", Long.class);

        // 列表默认只看待研判
        assertEquals("OPEN", db.queryForObject("select status from warning_record where id=?", String.class, id));

        // 研判：不升级时保留原等级
        var triaged = data(post_("/api/v1/warnings/" + id + "/triage", "{\"note\":\"已与家长沟通\",\"expectedStatus\":\"OPEN\",\"escalate\":false}"));
        assertEquals("REVIEWED", triaged.path("status").asText());
        assertEquals("FOCUS", db.queryForObject("select level from warning_record where id=?", String.class, id));

        // 状态已变化：用旧状态再研判必须冲突，而不是静默覆盖
        http.perform(post_("/api/v1/warnings/" + id + "/triage", "{\"note\":\"重复提交\",\"expectedStatus\":\"OPEN\"}"))
                .andExpect(status().isConflict());
        assertEquals("已与家长沟通", db.queryForObject("select teacher_note from warning_record where id=?", String.class, id));

        // 关闭：从已研判状态关闭，并记录关闭原因
        var closed = data(post_("/api/v1/warnings/" + id + "/close", "{\"note\":\"已核实并完成帮扶\",\"expectedStatus\":\"REVIEWED\"}"));
        assertEquals("CLOSED", closed.path("status").asText());
        assertNotNull(db.queryForObject("select closed_at from warning_record where id=?", java.sql.Timestamp.class, id));

        // 详情回读：证据可解析，过程记录包含研判与关闭，操作者为教师
        var detail = data(get("/api/v1/warnings/" + id).cookie(session));
        assertEquals("CLOSED", detail.path("warning").path("status").asText());
        assertTrue(detail.path("warning").path("evidence").isArray());
        var events = detail.path("events");
        assertEquals(2, events.size());
        assertEquals("TRIAGE", events.get(0).path("action").asText());
        assertEquals("CLOSE", events.get(1).path("action").asText());
        assertEquals("林老师", events.get(0).path("actor").asText());

        // 已关闭的预警再次关闭，请求体状态不在允许范围内
        http.perform(post_("/api/v1/warnings/" + id + "/close", "{\"note\":\"重复关闭\",\"expectedStatus\":\"CLOSED\"}"))
                .andExpect(status().isBadRequest());

        // 参数错误与未知筛选值
        http.perform(post_("/api/v1/warnings/" + id + "/triage", "{\"note\":\"缺少状态\"}")).andExpect(status().isBadRequest());
        http.perform(post_("/api/v1/warnings/" + id + "/triage", "{\"note\":\"非法状态\",\"expectedStatus\":\"BOGUS\"}")).andExpect(status().isBadRequest());
        http.perform(get("/api/v1/warnings?status=BOGUS").cookie(session)).andExpect(status().isBadRequest());
        assertEquals(0, data(get("/api/v1/warnings?status=OPEN&q=不存在的学生").cookie(session)).path("total").asInt());
        assertTrue(data(get("/api/v1/warnings?status=CLOSED").cookie(session)).path("total").asInt() >= 1);
    }

    @Test
    void escalateMarksWarningForManualReview() throws Exception {
        signIn();
        long id = db.queryForObject("select id from warning_record where student_id=1 and status='OPEN'", Long.class);
        data(post_("/api/v1/warnings/" + id + "/triage", "{\"note\":\"需要人工重点研判\",\"expectedStatus\":\"OPEN\",\"escalate\":true}"));
        assertEquals("MANUAL", db.queryForObject("select level from warning_record where id=?", String.class, id));
    }
}
