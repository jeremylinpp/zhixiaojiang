package com.zhixiaojiang;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Isolated H2 smoke test for the end-to-end business path. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ApiSmokeTest {
  @Autowired MockMvc http;
  @Autowired ObjectMapper json;

  @Test
  void loginDashboardAndBusinessWrites() throws Exception {
    var login = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"teacher\",\"password\":\"password\"}"))
      .andExpect(status().isOk()).andReturn();
    Cookie session = login.getResponse().getCookie("ZJ_SESSION"); assertNotNull(session);
    http.perform(get("/api/v1/dashboard/class").cookie(session)).andExpect(status().isOk());
    var students = http.perform(get("/api/v1/students").param("page", "2").param("pageSize", "5").cookie(session)).andExpect(status().isOk()).andReturn();
    var studentData = json.readTree(students.getResponse().getContentAsString()).path("data");
    assertEquals(42, studentData.path("total").asInt()); assertEquals(5, studentData.path("items").size()); assertEquals(2, studentData.path("page").asInt());
    var csrfResponse = http.perform(get("/api/v1/auth/csrf").cookie(session)).andExpect(status().isOk()).andReturn();
    String csrfToken = json.readTree(csrfResponse.getResponse().getContentAsString()).path("data").path("token").asText();
    Cookie csrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN"); assertFalse(csrfToken.isBlank()); assertNotNull(csrfCookie);
    var attendance = http.perform(get("/api/v1/students/1/attendance").cookie(session)).andExpect(status().isOk()).andReturn();
    assertEquals(1, json.readTree(attendance.getResponse().getContentAsString()).path("data").path("items").size());
    http.perform(secure(post("/api/v1/students/1/attendance"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content("{\"attendanceDate\":\"2026-09-14\",\"status\":\"LATE\"}")).andExpect(status().isOk());
    var rules = http.perform(get("/api/v1/point-rules").cookie(session)).andExpect(status().isOk()).andReturn();
    assertEquals(3, json.readTree(rules.getResponse().getContentAsString()).path("data").path("items").size());
    var ai = http.perform(secure(post("/api/v1/ai/student-analysis"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":1,\"scores\":[78,70,63,58],\"attendance\":{\"late\":3}}" )).andExpect(status().isOk()).andReturn();
    assertEquals("TEMPLATE", json.readTree(ai.getResponse().getContentAsString()).path("data").path("source").asText());
    String point = "{\"studentId\":1,\"amount\":1,\"reason\":\"smoke\",\"idempotencyKey\":\"test-point-1\"}";
    http.perform(post("/api/v1/points").cookie(session,csrfCookie).header("X-CSRF-TOKEN",csrfToken).contentType(MediaType.APPLICATION_JSON).content(point)).andExpect(status().isOk());
    var duplicate = http.perform(post("/api/v1/points").cookie(session,csrfCookie).header("X-CSRF-TOKEN",csrfToken).contentType(MediaType.APPLICATION_JSON).content(point)).andExpect(status().isOk()).andReturn();
    assertTrue(duplicate.getResponse().getContentAsString().contains("\"saved\":false"));
    var planResponse = http.perform(secure(post("/api/v1/interventions"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":1,\"title\":\"smoke plan\"}")).andExpect(status().isOk()).andReturn();
    long planId = json.readTree(planResponse.getResponse().getContentAsString()).path("data").path("id").asLong(); assertTrue(planId > 0);
    for (String next : new String[]{"CONFIRMED","IN_PROGRESS","COMPLETED"}) http.perform(secure(post("/api/v1/interventions/"+planId+"/transition"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\""+next+"\"}" )).andExpect(status().isOk());
    var taskResponse = http.perform(secure(post("/api/v1/growth-tasks"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content("{\"module\":\"聚机力\",\"title\":\"smoke task\",\"pointReward\":1}")).andExpect(status().isOk()).andReturn();
    long taskId = json.readTree(taskResponse.getResponse().getContentAsString()).path("data").path("id").asLong();
    http.perform(secure(post("/api/v1/growth-tasks/"+taskId+"/assign"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content("{\"studentIds\":[1]}")).andExpect(status().isOk());
    var assigned = http.perform(get("/api/v1/growth-tasks/"+taskId+"/students").cookie(session)).andExpect(status().isOk()).andReturn();
    long studentTaskId = json.readTree(assigned.getResponse().getContentAsString()).path("data").path("items").get(0).path("id").asLong();
    http.perform(secure(post("/api/v1/student-tasks/"+studentTaskId+"/complete"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    http.perform(secure(post("/api/v1/class-diagnoses"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"smoke target\",\"targetValue\":90,\"currentValue\":80}")).andExpect(status().isOk());
    var chinese = http.perform(get("/api/v1/students").param("q","小智").cookie(session)).andExpect(status().isOk()).andReturn();
    assertEquals("小智", json.readTree(chinese.getResponse().getContentAsByteArray()).path("data").path("items").get(0).path("name").asText());
    String studentBody = "{\"name\":\"中文验证学生\",\"studentNo\":\"UTF8-TEST\",\"gender\":\"女\"}";
    var created = http.perform(secure(post("/api/v1/students"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content(studentBody)).andExpect(status().isOk()).andReturn();
    long sid = json.readTree(created.getResponse().getContentAsByteArray()).path("data").path("id").asLong();
    assertTrue(sid > 42);
    http.perform(secure(put("/api/v1/students/"+sid),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content(studentBody.replace("中文验证学生","已修订学生"))).andExpect(status().isOk());
    var revised = http.perform(get("/api/v1/students/"+sid).cookie(session)).andExpect(status().isOk()).andReturn();
    assertEquals("已修订学生",json.readTree(revised.getResponse().getContentAsByteArray()).path("data").path("student").path("name").asText());
    http.perform(secure(post("/api/v1/students/"+sid+"/archive"),session,csrfCookie,csrfToken)).andExpect(status().isOk());
    var archived = http.perform(get("/api/v1/students/"+sid).cookie(session)).andExpect(status().isOk()).andReturn();
    assertEquals("ARCHIVED",json.readTree(archived.getResponse().getContentAsByteArray()).path("data").path("student").path("status").asText());
    http.perform(secure(post("/api/v1/students"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \",\"studentNo\":\"X\"}")).andExpect(status().isBadRequest());
    var profileResponse = http.perform(get("/api/v1/profile").cookie(session)).andExpect(status().isOk()).andReturn();
    var profileData = json.readTree(profileResponse.getResponse().getContentAsByteArray()).path("data");
    assertEquals("teacher",profileData.path("teacher").path("username").asText());
    assertEquals(1,profileData.path("classes").size());
    String oldName=profileData.path("teacher").path("displayName").asText();
    String profileUpdate=json.writeValueAsString(java.util.Map.of("displayName","测试教师名称","previousDisplayName",oldName));
    http.perform(secure(put("/api/v1/profile"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content(profileUpdate)).andExpect(status().isOk());
    var profileSaved=http.perform(get("/api/v1/profile").cookie(session)).andExpect(status().isOk()).andReturn();
    assertEquals("测试教师名称",json.readTree(profileSaved.getResponse().getContentAsByteArray()).path("data").path("teacher").path("displayName").asText());
    http.perform(secure(put("/api/v1/profile"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content(profileUpdate)).andExpect(status().isConflict());
    http.perform(secure(put("/api/v1/profile"),session,csrfCookie,csrfToken).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("displayName",oldName,"previousDisplayName","测试教师名称")))).andExpect(status().isOk());
    var logout = http.perform(secure(post("/api/v1/auth/logout"),session,csrfCookie,csrfToken)).andExpect(status().isOk()).andReturn();
    assertEquals(0, logout.getResponse().getCookie("ZJ_SESSION").getMaxAge());
  }

  private MockHttpServletRequestBuilder secure(MockHttpServletRequestBuilder request, Cookie session, Cookie csrfCookie, String token) {
    return request.cookie(session, csrfCookie).header("X-CSRF-TOKEN", token);
  }
}
