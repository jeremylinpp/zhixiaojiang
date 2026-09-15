package com.zhixiaojiang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JSON 键名契约：接口返回的键必须是前端使用的 camelCase。
 *
 * <p>历史问题：DAO 用 SQL 别名（{@code as studentNo}）返回字段时，H2（demo profile）会把未加引号的
 * 别名折叠成小写、MySQL 保留大小写，导致同一接口在两套数据库下键名不一致、页面取值变成 undefined。
 * 现在 SQL 只写库内真实列名，由 RowMaps 统一转 camelCase；本测试在 H2 下锁定这些键名。
 */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class JsonKeyContractTest {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    Cookie session;

    private void signIn() throws Exception {
        session = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"teacher\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("ZJ_SESSION");
    }

    private JsonNode data(MockHttpServletRequestBuilder request) throws Exception {
        return json.readTree(http.perform(request.cookie(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }

    @Test
    void camelCaseKeysAcrossDomains() throws Exception {
        signIn();

        var students = data(get("/api/v1/students?pageSize=3"));
        var student = students.path("items").get(0);
        assertTrue(student.has("studentNo"), "学生列表缺少 studentNo：" + student);
        assertTrue(student.has("growthIndex"), "学生列表缺少 growthIndex：" + student);
        assertTrue(students.has("page") && students.has("pageSize") && students.has("total"));

        var dashboard = data(get("/api/v1/dashboard/class"));
        assertTrue(dashboard.path("metrics").has("studentCount"));
        assertTrue(dashboard.path("metrics").has("attendanceDate"));
        assertTrue(dashboard.path("metrics").has("activeRate"));
        assertTrue(dashboard.path("metrics").has("attendanceCompleteness"));
        assertTrue(dashboard.path("targets").get(0).has("targetValue"), "缺少 targetValue：" + dashboard.path("targets").get(0));
        assertTrue(dashboard.path("targets").get(0).has("currentValue"));
        assertTrue(dashboard.path("warnings").get(0).has("studentName"));

        var workspace = data(get("/api/v1/students/1/growth-workspace"));
        assertTrue(workspace.path("student").has("studentNo"), "成长工作台缺少 studentNo：" + workspace.path("student"));
        assertTrue(workspace.path("scores").get(0).has("examName"));
        assertTrue(workspace.path("scores").get(0).has("fullScore"));
        assertTrue(workspace.path("scores").get(0).has("occurredOn"));
        assertTrue(workspace.has("growthIndex") && workspace.has("dimensions"));

        var tasks = data(get("/api/v1/growth-tasks")).path("items").get(0);
        assertTrue(tasks.has("dueOn"), "六机任务缺少 dueOn：" + tasks);
        assertTrue(tasks.has("pointReward"));

        var diagnosis = data(get("/api/v1/class-diagnoses")).path("items").get(0);
        assertTrue(diagnosis.has("targetValue") && diagnosis.has("currentValue") && diagnosis.has("deviation"), "诊改指标键名不符：" + diagnosis);

        var plan = data(get("/api/v1/interventions")).path("items").get(0);
        assertTrue(plan.has("studentName") && plan.has("teacherNote") && plan.has("reviewAt"), "帮扶方案键名不符：" + plan);

        var planDetail = data(get("/api/v1/interventions/1"));
        assertTrue(planDetail.path("plan").has("studentId") && planDetail.path("plan").has("warningId") && planDetail.path("plan").has("suggestions"));

        var warning = data(get("/api/v1/warnings?status=ALL")).path("items").get(0);
        assertTrue(warning.has("ruleCode") && warning.has("studentName") && warning.has("teacherNote") && warning.path("evidence").isArray(), "预警键名不符：" + warning);

        var ledger = data(get("/api/v1/students/1/points"));
        assertTrue(ledger.has("balance") && ledger.has("total") && ledger.has("page") && ledger.path("items").isArray());
        // 单条流水的 createdAt / reversed / reversalOf 键名由 PointsControllerTest 覆盖（那里会先入账一笔）

        var profile = data(get("/api/v1/profile"));
        assertTrue(profile.path("teacher").has("displayName"));
        assertTrue(profile.path("classes").get(0).has("studentCount") && profile.path("classes").get(0).has("isDemo"));
    }
}
