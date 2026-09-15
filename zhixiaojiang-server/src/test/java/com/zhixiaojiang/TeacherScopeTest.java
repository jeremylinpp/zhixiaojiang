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
 * 班级授权边界：第二个教师不得以任何方式读写第一个班级的学生、预警、帮扶、任务和诊改数据。
 *
 * <p>覆盖从「硬编码 class_id=1」收敛为「按 class_room.teacher_id 归属」之后的全部业务入口。
 */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class TeacherScopeTest {
    /** data.sql 中文末 UPDATE 写入的口令哈希，对应 teacher/password。 */
    private static final String DEMO_HASH = "$2a$10$n4DBvIrKTMNgcMfJ/bTzqOUTdU51cNN7GneXJ7t8OV3xgPxT13lhW";

    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;

    Cookie otherSession, otherCsrfCookie;
    String otherCsrf;
    Cookie ownSession, ownCsrfCookie;
    String ownCsrf;

    private record Session(Cookie cookie, Cookie csrfCookie, String csrf) {}

    private Session signIn(String username) throws Exception {
        var login = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn();
        Cookie session = login.getResponse().getCookie("ZJ_SESSION");
        assertNotNull(session, "登录未返回会话 Cookie：" + username);
        var csrfResponse = http.perform(get("/api/v1/auth/csrf").cookie(session)).andExpect(status().isOk()).andReturn().getResponse();
        return new Session(session, csrfResponse.getCookie("XSRF-TOKEN"),
                json.readTree(csrfResponse.getContentAsByteArray()).path("data").path("token").asText());
    }

    private MockHttpServletRequestBuilder as(Session who, MockHttpServletRequestBuilder request) {
        return request.cookie(who.cookie(), who.csrfCookie()).header("X-CSRF-TOKEN", who.csrf());
    }

    private MockHttpServletRequestBuilder json(Session who, MockHttpServletRequestBuilder request, String body) {
        return as(who, request).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private JsonNode data(MockHttpServletRequestBuilder request) throws Exception {
        return json.readTree(http.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }

    /** 第二个教师、第二个班级及其名下一套完整业务数据。 */
    private void seedSecondTeacher() {
        db.update("insert into sys_user(id,username,password_hash,display_name,role) values(200,'teacher2',?,'另一位老师','TEACHER')", DEMO_HASH);
        db.update("insert into class_room(id,name,grade,teacher_id,is_demo) values(200,'隔离班级','2026',200,true)");
        db.update("insert into student(id,class_id,student_no,name) values(200,200,'OTHER','其他班级学生')");
        db.update("insert into warning_record(id,student_id,level,rule_code,summary,evidence_json,status) values(200,200,'ATTENTION','LATE_14D','其他班级预警','[\"迟到次数=3\"]','OPEN')");
        db.update("insert into intervention_plan(id,student_id,title,status,suggestions_json,created_by) values(200,200,'其他班级方案','DRAFT','[]',200)");
        db.update("insert into class_target(id,class_id,name,target_value,current_value,created_by) values(200,200,'其他班级指标',90,80,200)");
        db.update("insert into growth_task(id,module,title,due_on,point_reward,status,created_by) values(200,'聚机力','其他班级任务',curdate(),1,'PUBLISHED',200)");
        db.update("insert into student_task(id,task_id,student_id,status) values(200,200,200,'ASSIGNED')");
        // 1 号班级的待办任务，用于验证跨班确认与发币
        db.update("insert into student_task(id,task_id,student_id,status) values(300,1,1,'ASSIGNED')");
    }

    @Test
    void secondTeacherCannotReachFirstClassData() throws Exception {
        seedSecondTeacher();
        var other = signIn("teacher2");
        var own = signIn("teacher");
        // 演示数据里的 1 号方案已是 CONFIRMED，越权流转不得改变它
        String planStatusBefore = db.queryForObject("select status from intervention_plan where id=1", String.class);

        // 学生列表与档案：只看到本班，跨班一律 404
        var students = data(as(other, get("/api/v1/students")));
        assertEquals(1, students.path("total").asInt());
        assertEquals("其他班级学生", students.path("items").get(0).path("name").asText());
        http.perform(as(other, get("/api/v1/students/200"))).andExpect(status().isOk());
        for (String path : new String[]{"/api/v1/students/1", "/api/v1/students/1/portrait", "/api/v1/students/1/timeline",
                "/api/v1/students/1/attendance", "/api/v1/students/1/behavior", "/api/v1/students/1/skills", "/api/v1/students/1/evaluations"})
            http.perform(as(other, get(path))).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/students/1/behavior"), "{\"category\":\"越权\",\"score\":1}")).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/students/1/growth"), "{\"dimension\":\"SMART\",\"score\":90,\"title\":\"越权\",\"source\":\"越权\",\"occurredOn\":\"" + java.time.LocalDate.now() + "\"}")).andExpect(status().isNotFound());
        http.perform(json(other, put("/api/v1/students/1"), "{\"name\":\"越权改名\",\"studentNo\":\"JZ001\"}")).andExpect(status().isNotFound());
        http.perform(as(other, post("/api/v1/students/1/archive"))).andExpect(status().isNotFound());

        // 反向：1 号教师也看不到 2 号班级的学生；同时记录基线，供最后核对未被污染
        int ownStudents = data(as(own, get("/api/v1/students"))).path("total").asInt();
        assertTrue(ownStudents >= 42, "演示班级应有在籍学生，实际 " + ownStudents);
        int ownTargets = data(as(own, get("/api/v1/class-diagnoses"))).path("items").size();
        int ownPlans = data(as(own, get("/api/v1/interventions"))).path("items").size();
        int ownTasks = data(as(own, get("/api/v1/growth-tasks"))).path("items").size();
        http.perform(as(own, get("/api/v1/students/200"))).andExpect(status().isNotFound());

        // 驾驶舱只统计自己班级
        var dashboard = data(as(other, get("/api/v1/dashboard/class")));
        assertEquals(1, dashboard.path("metrics").path("studentCount").asInt());
        assertEquals(1, dashboard.path("targets").size());
        assertEquals(1, dashboard.path("warnings").size());

        // 预警列表、研判与关闭
        assertEquals(1, data(as(other, get("/api/v1/warnings"))).path("total").asInt());
        http.perform(as(other, get("/api/v1/warnings/1"))).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/warnings/1/triage"), "{\"note\":\"越权研判\",\"expectedStatus\":\"OPEN\"}")).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/warnings/1/close"), "{\"note\":\"越权关闭\",\"expectedStatus\":\"OPEN\"}")).andExpect(status().isNotFound());

        // 帮扶：列表、创建、修改、流转、过程记录
        var plans = data(as(other, get("/api/v1/interventions"))).path("items");
        assertEquals(1, plans.size());
        assertEquals(200, plans.get(0).path("id").asLong());
        http.perform(json(other, post("/api/v1/interventions"), "{\"studentId\":1,\"title\":\"越权方案\"}")).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/interventions"), "{\"studentId\":200,\"warningId\":1,\"title\":\"挂到别人预警\"}")).andExpect(status().isNotFound());
        http.perform(as(other, get("/api/v1/interventions/1"))).andExpect(status().isNotFound());
        http.perform(json(other, put("/api/v1/interventions/1"), "{\"title\":\"越权改名\"}")).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/interventions/1/transition"), "{\"status\":\"CONFIRMED\"}")).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/interventions/1/records"), "{\"action\":\"越权记录\"}")).andExpect(status().isNotFound());
        assertEquals(planStatusBefore, db.queryForObject("select status from intervention_plan where id=1", String.class));

        // 六机任务：任务按发布者隔离，指派与完成都要双向校验
        var tasks = data(as(other, get("/api/v1/growth-tasks"))).path("items");
        assertEquals(1, tasks.size());
        assertEquals(200, tasks.get(0).path("id").asLong());
        http.perform(json(other, post("/api/v1/growth-tasks/1/assign"), "{\"studentIds\":[200]}")).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/growth-tasks/200/assign"), "{\"studentIds\":[1]}")).andExpect(status().isNotFound());
        http.perform(as(other, get("/api/v1/growth-tasks/1/students"))).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/student-tasks/300/complete"), "{}")).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/student-tasks/300/evaluate"), "{\"note\":\"越权评价\"}")).andExpect(status().isNotFound());
        assertEquals("ASSIGNED", db.queryForObject("select status from student_task where id=300", String.class));
        assertEquals(0, db.queryForObject("select count(*) from point_ledger where idempotency_key='task:300'", Integer.class));

        // 班级诊改：目标、措施记录、复评
        var targets = data(as(other, get("/api/v1/class-diagnoses"))).path("items");
        assertEquals(1, targets.size());
        assertEquals(200, targets.get(0).path("id").asLong());
        http.perform(as(other, get("/api/v1/class-diagnoses/1/records"))).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/class-diagnoses/1/records"), "{\"measure\":\"越权措施\"}")).andExpect(status().isNotFound());
        http.perform(json(other, put("/api/v1/class-diagnoses/1"), "{\"currentValue\":100}")).andExpect(status().isNotFound());
        http.perform(json(other, post("/api/v1/class-diagnoses/1/review"), "{\"currentValue\":100}")).andExpect(status().isNotFound());
        assertEquals(76, db.queryForObject("select current_value from class_target where id=1", Integer.class));

        // AI 分析只接受本班学生；1 号班级不得产生新的分析记录
        int aiBefore = db.queryForObject("select count(*) from ai_analysis where student_id=1", Integer.class);
        http.perform(json(other, post("/api/v1/ai/student-analysis"), "{\"studentId\":1,\"scores\":[78,70]}")).andExpect(status().isNotFound());
        assertEquals("TEMPLATE", data(json(other, post("/api/v1/ai/student-analysis"), "{\"studentId\":200,\"scores\":[60,50]}")).path("source").asText());
        assertEquals(aiBefore, db.queryForObject("select count(*) from ai_analysis where student_id=1", Integer.class));

        // 规则分析只遍历自己的学生，不产生 1 号班级的预警
        int openBefore = db.queryForObject("select count(*) from warning_record where student_id=1 and status='OPEN'", Integer.class);
        assertEquals(0, data(as(other, post("/api/v1/warnings/analyze"))).path("created").asInt());
        assertEquals(openBefore, db.queryForObject("select count(*) from warning_record where student_id=1 and status='OPEN'", Integer.class));

        // 新建学生与新建诊改目标落到自己的班级
        long created = data(json(other, post("/api/v1/students"), "{\"name\":\"新学生\",\"studentNo\":\"NEW-200\",\"gender\":\"女\"}")).path("id").asLong();
        assertEquals(200, db.queryForObject("select class_id from student where id=?", Long.class, created));
        long target = data(json(other, post("/api/v1/class-diagnoses"), "{\"name\":\"新指标\",\"targetValue\":95}")).path("id").asLong();
        assertEquals(200, db.queryForObject("select class_id from class_target where id=?", Long.class, target));

        // 1 号教师的既有数据未被越权操作污染
        assertEquals("小智", data(as(own, get("/api/v1/students/1"))).path("student").path("name").asText());
        assertEquals(ownStudents, data(as(own, get("/api/v1/students"))).path("total").asInt());
        assertEquals(ownTargets, data(as(own, get("/api/v1/class-diagnoses"))).path("items").size());
        assertEquals(ownPlans, data(as(own, get("/api/v1/interventions"))).path("items").size());
        assertEquals(ownTasks, data(as(own, get("/api/v1/growth-tasks"))).path("items").size());

        // 帮扶方案详情：建议列表、预警关联与执行过程记录可读（页面审核与复评所需）
        long linked = data(json(own, post("/api/v1/interventions"),
                "{\"studentId\":1,\"warningId\":1,\"title\":\"详情验证\",\"suggestions\":[\"班主任个别谈话\",\"两周后复评\"]}")).path("id").asLong();
        var detail = data(as(own, get("/api/v1/interventions/" + linked)));
        assertEquals("DRAFT", detail.path("plan").path("status").asText());
        assertEquals(2, detail.path("plan").path("suggestions").size());
        assertEquals(1, detail.path("plan").path("warningId").asLong());
        assertEquals(0, detail.path("records").size());
        http.perform(json(own, post("/api/v1/interventions/" + linked + "/records"), "{\"action\":\"班主任个别谈话\",\"result\":\"已完成\"}")).andExpect(status().isOk());
        assertEquals(1, data(as(own, get("/api/v1/interventions/" + linked))).path("records").size());
    }
}
