package com.zhixiaojiang;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:studentportal;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:demo-schema.sql,classpath:student-portal-schema.sql",
    "spring.sql.init.data-locations=classpath:demo-data.sql", "app.redis-enabled=false"
})
@ActiveProfiles("student-test")
@AutoConfigureMockMvc
class StudentPortalSecurityTest {
    @Autowired MockMvc http;
    @Autowired JdbcTemplate db;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    @Test
    void studentCannotReadTeacherOrPeerDataAndArchivedStudentIsRejected() throws Exception {
        db.update("insert into sys_user(id,username,password_hash,display_name,role) values(9001,'portal-test',?,'测试学生','STUDENT')", encoder.encode("test-password"));
        db.update("insert into student_account(user_id,student_id,must_change_password) values(9001,1,false)");
        var login = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"portal-test\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andReturn();
        Cookie session = login.getResponse().getCookie("ZJ_SESSION");
        assertNotNull(session);
        http.perform(get("/api/v1/student-portal/home")).andExpect(status().isUnauthorized());
        for (String path : new String[]{"/api/v1/students", "/api/v1/students/2", "/api/v1/warnings", "/api/v1/profile"})
            http.perform(get(path).cookie(session)).andExpect(status().isForbidden());
        var home = http.perform(get("/api/v1/student-portal/home").param("studentId", "2").cookie(session))
                .andExpect(status().isOk()).andReturn();
        var data = json.readTree(home.getResponse().getContentAsByteArray()).path("data");
        assertEquals(db.queryForObject("select name from student where id=1", String.class), data.path("student").path("name").asText());
        assertFalse(data.toString().contains("teacherNote"));
        var ledger = http.perform(get("/api/v1/student-portal/points").param("studentId", "2").cookie(session))
                .andExpect(status().isOk()).andReturn();
        assertEquals(db.queryForObject("select coalesce(sum(amount),0) from point_ledger where student_id=1", Long.class).longValue(),
                json.readTree(ledger.getResponse().getContentAsByteArray()).path("data").path("balance").asLong());
        var teacherLogin = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"teacher\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn();
        http.perform(get("/api/v1/student-portal/home").cookie(teacherLogin.getResponse().getCookie("ZJ_SESSION")))
                .andExpect(status().isForbidden());
        http.perform(get("/api/v1/students").cookie(teacherLogin.getResponse().getCookie("ZJ_SESSION")))
                .andExpect(status().isOk());
        taskLifecycle(session, teacherLogin.getResponse().getCookie("ZJ_SESSION"));
        growthReview(session, teacherLogin.getResponse().getCookie("ZJ_SESSION"));
        db.update("update student set status='ARCHIVED' where id=1");
        http.perform(get("/api/v1/student-portal/home").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void teacherProvisioningAndMandatoryPasswordChangeInvalidateEveryOldSession() throws Exception {
        Cookie teacher=login("teacher","password");
        var csrfResponse=http.perform(get("/api/v1/auth/csrf").cookie(teacher)).andReturn();
        Cookie csrfCookie=csrfResponse.getResponse().getCookie("XSRF-TOKEN");
        String csrf=json.readTree(csrfResponse.getResponse().getContentAsByteArray()).path("data").path("token").asText();
        String body="{\"username\":\"student-three\",\"initialPassword\":\"initial-test-password\"}";
        var created=http.perform(post("/api/v1/students/3/account").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();
        assertFalse(created.getResponse().getContentAsString().contains("initial-test-password"));
        http.perform(post("/api/v1/students/3/account").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        Cookie first=login("student-three","initial-test-password"),second=login("student-three","initial-test-password");
        http.perform(get("/api/v1/student-portal/home").cookie(first)).andExpect(status().isForbidden());
        var me=http.perform(get("/api/v1/auth/me").cookie(first)).andExpect(status().isOk()).andReturn();
        assertTrue(json.readTree(me.getResponse().getContentAsByteArray()).path("data").path("mustChangePassword").asBoolean());
        http.perform(post("/api/v1/student-portal/password").cookie(first,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"wrong\",\"newPassword\":\"new-test-password\"}")).andExpect(status().isBadRequest());
        http.perform(post("/api/v1/student-portal/password").cookie(first,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"initial-test-password\",\"newPassword\":\"new-test-password\"}")).andExpect(status().isOk());
        for(Cookie old:new Cookie[]{first,second})http.perform(get("/api/v1/auth/me").cookie(old)).andExpect(status().isUnauthorized());
        Cookie current=login("student-three","new-test-password");
        http.perform(get("/api/v1/student-portal/home").cookie(current)).andExpect(status().isOk());
        http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"student-three\",\"password\":\"initial-test-password\"}")).andExpect(status().isUnauthorized());
        http.perform(post("/api/v1/students/4/account").cookie(current,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        assertTrue(db.queryForObject("select password_hash from sys_user where username='student-three'",String.class).startsWith("$2"));
    }

    private Cookie login(String username,String password) throws Exception {
        return http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("username",username,"password",password))))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("ZJ_SESSION");
    }

    @Test
    void attachmentsAreOwnedImmutableBoundAndIdempotent() throws Exception {
        for(int studentId:new int[]{7,8}){
            db.update("insert into sys_user(id,username,password_hash,display_name,role) values(?,?,?,'附件测试学生','STUDENT')",9000+studentId,"file-student-"+studentId,encoder.encode("test-file-password"));
            db.update("insert into student_account(user_id,student_id,must_change_password) values(?,?,false)",9000+studentId,studentId);
        }
        Cookie student=login("file-student-7","test-file-password"),peer=login("file-student-8","test-file-password"),teacher=login("teacher","password");
        var csrfResponse=http.perform(get("/api/v1/auth/csrf").cookie(student)).andReturn();
        Cookie csrfCookie=csrfResponse.getResponse().getCookie("XSRF-TOKEN");
        String csrf=json.readTree(csrfResponse.getResponse().getContentAsByteArray()).path("data").path("token").asText();
        db.update("insert into growth_task(id,module,title,due_on,point_reward,status,created_by) values(9952,'聚机力','附件测试',current_date,1,'PUBLISHED',1)");
        db.update("insert into student_task(id,task_id,student_id,status) values(9952,9952,7,'ASSIGNED'),(9953,9952,8,'ASSIGNED')");
        byte[] png=java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a5h8AAAAASUVORK5CYII=");
        var file=new org.springframework.mock.web.MockMultipartFile("file","成果.png","image/png",png);
        http.perform(multipart("/api/v1/student-portal/tasks/9952/attachments").file(file).param("requestKey","one").cookie(student)).andExpect(status().isForbidden());
        var uploaded=http.perform(multipart("/api/v1/student-portal/tasks/9952/attachments").file(file).param("requestKey","one").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf)).andExpect(status().isOk()).andReturn();
        long id=json.readTree(uploaded.getResponse().getContentAsByteArray()).path("data").path("id").asLong();
        http.perform(multipart("/api/v1/student-portal/tasks/9952/attachments").file(file).param("requestKey","one").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf)).andExpect(status().isOk());
        assertEquals(1,db.queryForObject("select count(*) from task_attachment where student_task_id=9952",Integer.class));
        http.perform(get("/api/v1/student-portal/attachments/"+id).cookie(peer)).andExpect(status().isNotFound());
        http.perform(get("/api/v1/task-attachments/"+id).cookie(teacher)).andExpect(status().isNotFound());
        var downloaded=http.perform(get("/api/v1/student-portal/attachments/"+id).cookie(student)).andExpect(status().isOk()).andReturn();
        assertArrayEquals(png,downloaded.getResponse().getContentAsByteArray());
        assertEquals("nosniff",downloaded.getResponse().getHeader("X-Content-Type-Options"));
        assertTrue(downloaded.getResponse().getHeader("Content-Disposition").startsWith("attachment;"));
        var fake=new org.springframework.mock.web.MockMultipartFile("file","bad.png","image/png","<script>alert(1)</script>".getBytes());
        http.perform(multipart("/api/v1/student-portal/tasks/9952/attachments").file(fake).param("requestKey","fake").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf)).andExpect(status().isBadRequest());
        var huge=new org.springframework.mock.web.MockMultipartFile("file","big.png","image/png",new byte[2*1024*1024+1]);
        http.perform(multipart("/api/v1/student-portal/tasks/9952/attachments").file(huge).param("requestKey","big").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf)).andExpect(status().isPayloadTooLarge());
        String peerBody="{\"requestKey\":\"steal-file\",\"content\":\"成果\",\"attachmentIds\":["+id+"]}";
        http.perform(post("/api/v1/student-portal/tasks/9953/submissions").cookie(peer,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(peerBody)).andExpect(status().isConflict());
        assertEquals(0,db.queryForObject("select count(*) from task_submission where student_task_id=9953",Integer.class));
        String body="{\"requestKey\":\"with-file\",\"content\":\"成果\",\"attachmentIds\":["+id+"]}";
        for(int i=0;i<2;i++)http.perform(post("/api/v1/student-portal/tasks/9952/submissions").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        http.perform(delete("/api/v1/student-portal/attachments/"+id).cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf)).andExpect(status().isConflict());
        var teacherFile=http.perform(get("/api/v1/task-attachments/"+id).cookie(teacher)).andExpect(status().isOk()).andReturn();
        assertArrayEquals(png,teacherFile.getResponse().getContentAsByteArray());
        var history=http.perform(get("/api/v1/student-portal/tasks/9952/submissions").cookie(student)).andExpect(status().isOk()).andReturn();
        assertEquals(id,json.readTree(history.getResponse().getContentAsByteArray()).path("data").path("items").get(0).path("attachments").get(0).path("id").asLong());
        http.perform(post("/api/v1/student-portal/tasks/9952/submissions").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(body.replace("["+id+"]","[]"))).andExpect(status().isConflict());
    }

    @Test
    void publishedPlansKeepInternalNotesPrivateAndMessagesAreOwned() throws Exception {
        for(int studentId:new int[]{5,6}){
            db.update("insert into sys_user(id,username,password_hash,display_name,role) values(?,?,?,'计划测试学生','STUDENT')",9000+studentId,"plan-student-"+studentId,encoder.encode("test-plan-password"));
            db.update("insert into student_account(user_id,student_id,must_change_password) values(?,?,false)",9000+studentId,studentId);
        }
        Cookie student=login("plan-student-5","test-plan-password"),peer=login("plan-student-6","test-plan-password"),teacher=login("teacher","password");
        var csrfResponse=http.perform(get("/api/v1/auth/csrf").cookie(teacher)).andReturn();
        Cookie csrfCookie=csrfResponse.getResponse().getCookie("XSRF-TOKEN");
        String csrf=json.readTree(csrfResponse.getResponse().getContentAsByteArray()).path("data").path("token").asText();
        db.update("insert into intervention_plan(id,student_id,title,status,suggestions_json,teacher_note,created_by) values(9951,5,'PRIVATE TITLE','DRAFT','[]','PRIVATE CONVERSATION',1)");
        String publication="{\"expectedVersion\":0,\"title\":\"每周成长目标\",\"goal\":\"练习技能\",\"actions\":\"每天练习十分钟\",\"reviewOn\":\"2026-12-01\"}";
        http.perform(post("/api/v1/interventions/9951/student-publications").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(publication)).andExpect(status().isConflict());
        db.update("update intervention_plan set status='CONFIRMED' where id=9951");
        var response=http.perform(post("/api/v1/interventions/9951/student-publications").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(publication)).andExpect(status().isOk()).andReturn();
        long id=json.readTree(response.getResponse().getContentAsByteArray()).path("data").path("id").asLong();
        http.perform(post("/api/v1/interventions/9951/student-publications").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(publication)).andExpect(status().isConflict());
        var visible=http.perform(get("/api/v1/student-portal/plans").cookie(student)).andExpect(status().isOk()).andReturn();
        assertFalse(visible.getResponse().getContentAsString().contains("PRIVATE"));
        assertFalse(visible.getResponse().getContentAsString().contains("teacherNote"));
        http.perform(get("/api/v1/student-portal/plans/"+id).cookie(peer)).andExpect(status().isNotFound());
        String entry="{\"requestKey\":\"execution-one\",\"content\":\"完成今天的练习\",\"occurredOn\":\""+java.time.LocalDate.now()+"\"}";
        for(int i=0;i<2;i++)http.perform(post("/api/v1/student-portal/plans/"+id+"/executions").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(entry)).andExpect(status().isOk());
        assertEquals(1,db.queryForObject("select count(*) from student_plan_execution where publication_id=?",Integer.class,id));
        String feedback="{\"requestKey\":\"feedback-one\",\"content\":\"过程有进步，继续保持\"}";
        for(int i=0;i<2;i++)http.perform(post("/api/v1/student-plan-publications/"+id+"/feedback").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(feedback)).andExpect(status().isOk());
        var messages=http.perform(get("/api/v1/student-portal/messages").cookie(student)).andExpect(status().isOk()).andReturn();
        var messageData=json.readTree(messages.getResponse().getContentAsByteArray()).path("data");
        assertEquals(2,messageData.path("total").asInt());assertEquals(2,messageData.path("unread").asInt());
        long messageId=messageData.path("items").get(0).path("id").asLong();
        http.perform(post("/api/v1/student-portal/messages/"+messageId+"/read").cookie(peer,csrfCookie).header("X-CSRF-TOKEN",csrf)).andExpect(status().isNotFound());
        for(int i=0;i<2;i++)http.perform(post("/api/v1/student-portal/messages/"+messageId+"/read").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf)).andExpect(status().isOk());
        assertEquals(1,db.queryForObject("select count(*) from student_message where student_id=5 and read_at is null",Integer.class));
        var detail=http.perform(get("/api/v1/student-portal/plans/"+id).cookie(student)).andExpect(status().isOk()).andReturn();
        assertEquals(1,json.readTree(detail.getResponse().getContentAsByteArray()).path("data").path("feedback").size());
        var secondPublication=http.perform(post("/api/v1/interventions/9951/student-publications").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(publication.replace("\"expectedVersion\":0","\"expectedVersion\":1").replace("每天练习十分钟","每天练习十五分钟"))).andExpect(status().isOk()).andReturn();
        long latestId=json.readTree(secondPublication.getResponse().getContentAsByteArray()).path("data").path("id").asLong();
        var latest=http.perform(get("/api/v1/student-portal/plans/"+latestId).cookie(student)).andExpect(status().isOk()).andReturn();
        var latestData=json.readTree(latest.getResponse().getContentAsByteArray()).path("data");
        assertEquals(2,latestData.path("latestVersion").asInt());
        assertEquals(2,latestData.path("versions").size());
        assertEquals(id,latestData.path("versions").get(1).path("id").asLong());
        assertFalse(latestData.toString().contains("PRIVATE"));
        assertEquals(0,latestData.path("executions").size());
        var old=http.perform(get("/api/v1/student-portal/plans/"+id).cookie(student)).andExpect(status().isOk()).andReturn();
        var oldData=json.readTree(old.getResponse().getContentAsByteArray()).path("data");
        assertEquals(1,oldData.path("executions").size());
        assertEquals(1,oldData.path("feedback").size());
        http.perform(get("/api/v1/student-portal/plans/"+latestId).cookie(peer)).andExpect(status().isNotFound());
        http.perform(post("/api/v1/student-portal/plans/"+id+"/executions").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(entry.replace("execution-one","obsolete-version-entry"))).andExpect(status().isConflict());
        db.update("update intervention_plan set status='COMPLETED' where id=9951");
        http.perform(post("/api/v1/student-portal/plans/"+id+"/executions").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(entry.replace("execution-one","execution-two"))).andExpect(status().isConflict());
        assertEquals(0,db.queryForObject("select count(*) from point_ledger where student_id=5",Integer.class));
    }

    private void growthReview(Cookie student,Cookie teacher) throws Exception {
        var csrfResponse=http.perform(get("/api/v1/auth/csrf").cookie(student)).andReturn();
        Cookie csrfCookie=csrfResponse.getResponse().getCookie("XSRF-TOKEN");
        String csrf=json.readTree(csrfResponse.getResponse().getContentAsByteArray()).path("data").path("token").asText();
        String date=java.time.LocalDate.now().toString();
        String body="{\"requestKey\":\"growth-first\",\"category\":\"ACTIVITY\",\"title\":\"参与技能活动\",\"content\":\"完成小组作品\",\"occurredOn\":\""+date+"\"}";
        long activities=db.queryForObject("select count(*) from activity_record where student_id=1",Long.class);
        long dimensions=db.queryForObject("select count(*) from dimension_evaluation where student_id=1",Long.class);
        var submitted=http.perform(post("/api/v1/student-portal/growth").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();
        long id=json.readTree(submitted.getResponse().getContentAsByteArray()).path("data").path("id").asLong();
        http.perform(post("/api/v1/student-portal/growth").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        assertEquals(activities,db.queryForObject("select count(*) from activity_record where student_id=1",Long.class));
        http.perform(post("/api/v1/growth-submissions/"+id+"/review").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\",\"feedback\":\"确认参与\"}")).andExpect(status().isForbidden());
        for(int i=0;i<2;i++)http.perform(post("/api/v1/growth-submissions/"+id+"/review").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\",\"feedback\":\"确认参与\"}")).andExpect(status().isOk());
        assertEquals(activities+1,db.queryForObject("select count(*) from activity_record where student_id=1",Long.class));
        assertEquals(dimensions,db.queryForObject("select count(*) from dimension_evaluation where student_id=1",Long.class));
        var workspace=http.perform(get("/api/v1/student-portal/growth").param("studentId","2").cookie(student)).andExpect(status().isOk()).andReturn();
        var data=json.readTree(workspace.getResponse().getContentAsByteArray()).path("data");
        assertEquals("APPROVED",data.path("records").get(0).path("status").asText());
        assertFalse(data.toString().contains("passwordHash"));
        assertFalse(data.has("warnings"));
        String revisionBody=body.replace("growth-first","growth-returned");
        var returned=http.perform(post("/api/v1/student-portal/growth").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(revisionBody)).andExpect(status().isOk()).andReturn();
        long oldId=json.readTree(returned.getResponse().getContentAsByteArray()).path("data").path("id").asLong();
        http.perform(post("/api/v1/growth-submissions/"+oldId+"/review").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RETURNED\",\"feedback\":\"请补充过程\"}")).andExpect(status().isOk());
        var revision=json.readTree(body).deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode)revision).put("requestKey","growth-revision").put("previousId",oldId).put("content","补充了过程与收获");
        for(int i=0;i<2;i++)http.perform(post("/api/v1/student-portal/growth").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(revision.toString())).andExpect(status().isOk());
        assertEquals(1,db.queryForObject("select count(*) from student_growth_revision where previous_id=?",Integer.class,oldId));
        assertEquals("RETURNED",db.queryForObject("select status from student_growth_submission where id=?",String.class,oldId));
        assertEquals("完成小组作品",db.queryForObject("select content from student_growth_submission where id=?",String.class,oldId));
        ((com.fasterxml.jackson.databind.node.ObjectNode)revision).put("requestKey","growth-revision-again");
        http.perform(post("/api/v1/student-portal/growth").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(revision.toString())).andExpect(status().isConflict());
        ((com.fasterxml.jackson.databind.node.ObjectNode)revision).put("previousId",id);
        http.perform(post("/api/v1/student-portal/growth").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(revision.toString())).andExpect(status().isConflict());
        ((com.fasterxml.jackson.databind.node.ObjectNode)revision).put("previousId",999999);
        http.perform(post("/api/v1/student-portal/growth").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(revision.toString())).andExpect(status().isNotFound());
        http.perform(get("/api/v1/student-portal/growth").param("from","2026-12-31").param("to","2026-01-01").cookie(student)).andExpect(status().isBadRequest());
    }

    private void taskLifecycle(Cookie student, Cookie teacher) throws Exception {
        var csrfResponse=http.perform(get("/api/v1/auth/csrf").cookie(student)).andReturn();
        Cookie csrfCookie=csrfResponse.getResponse().getCookie("XSRF-TOKEN");
        String csrf=json.readTree(csrfResponse.getResponse().getContentAsByteArray()).path("data").path("token").asText();
        db.update("insert into growth_task(id,module,title,due_on,point_reward,status,created_by) values(9901,'聚机力','闭环任务',current_date,3,'PUBLISHED',1)");
        db.update("insert into student_task(id,task_id,student_id,status) values(9901,9901,1,'ASSIGNED'),(9902,9901,2,'ASSIGNED')");
        long balance=db.queryForObject("select coalesce(sum(amount),0) from point_ledger where student_id=1",Long.class);
        long activities=db.queryForObject("select count(*) from activity_record where student_id=1",Long.class);
        String first="{\"requestKey\":\"first\",\"content\":\"第一次成果\"}";
        assertTodo(student,9901,true);
        http.perform(get("/api/v1/student-portal/tasks/9902/submissions").cookie(student)).andExpect(status().isNotFound());
        http.perform(post("/api/v1/student-portal/tasks/9901/submissions").cookie(student).contentType(MediaType.APPLICATION_JSON).content(first)).andExpect(status().isForbidden());
        var saved=http.perform(post("/api/v1/student-portal/tasks/9901/submissions").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(first)).andExpect(status().isOk()).andReturn();
        long firstId=json.readTree(saved.getResponse().getContentAsByteArray()).path("data").path("id").asLong();
        assertTodo(student,9901,false);
        http.perform(post("/api/v1/student-portal/tasks/9901/submissions").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(first)).andExpect(status().isOk());
        assertEquals(1,db.queryForObject("select count(*) from task_submission where student_task_id=9901",Integer.class));
        http.perform(post("/api/v1/student-portal/tasks/9901/submissions").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(first.replace("第一次成果","修改内容"))).andExpect(status().isConflict());
        http.perform(post("/api/v1/student-tasks/9901/return").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content("{\"submissionId\":"+firstId+",\"feedback\":\"请补充过程\"}")).andExpect(status().isOk());
        assertTodo(student,9901,true);
        http.perform(post("/api/v1/student-tasks/9901/complete").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isConflict());
        var second=http.perform(post("/api/v1/student-portal/tasks/9901/submissions").cookie(student,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content("{\"requestKey\":\"second\",\"content\":\"补充后的完整成果\"}")).andExpect(status().isOk()).andReturn();
        long secondId=json.readTree(second.getResponse().getContentAsByteArray()).path("data").path("id").asLong();
        http.perform(post("/api/v1/student-tasks/9901/complete").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content("{\"submissionId\":"+firstId+"}")).andExpect(status().isConflict());
        for(int i=0;i<2;i++) http.perform(post("/api/v1/student-tasks/9901/complete").cookie(teacher,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content("{\"submissionId\":"+secondId+",\"note\":\"过程完整，通过\"}")).andExpect(status().isOk());
        assertEquals(balance+3,db.queryForObject("select coalesce(sum(amount),0) from point_ledger where student_id=1",Long.class));
        assertEquals(activities+1,db.queryForObject("select count(*) from activity_record where student_id=1",Long.class));
        assertTodo(student,9901,false);
        var history=http.perform(get("/api/v1/student-portal/tasks/9901/submissions").cookie(student)).andExpect(status().isOk()).andReturn();
        var rows=json.readTree(history.getResponse().getContentAsByteArray()).path("data").path("items");
        assertEquals(2,rows.size());
        assertEquals("COMPLETED",rows.get(0).path("status").asText());
        assertEquals("过程完整，通过",rows.get(0).path("feedback").asText());
        assertEquals("RETURNED",rows.get(1).path("status").asText());
    }

    private void assertTodo(Cookie student,long taskId,boolean expected) throws Exception {
        var response=http.perform(get("/api/v1/student-portal/home").cookie(student)).andExpect(status().isOk()).andReturn();
        var todos=json.readTree(response.getResponse().getContentAsByteArray()).path("data").path("todos");
        boolean found=false;
        for(var task:todos)if(task.path("id").asLong()==taskId)found=true;
        assertEquals(expected,found,"首页只列出需要学生操作的任务");
    }
}
