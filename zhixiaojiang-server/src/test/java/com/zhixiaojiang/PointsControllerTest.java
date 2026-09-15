package com.zhixiaojiang;

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

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class PointsControllerTest {
  @Autowired MockMvc http;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate db;
  Cookie session,csrfCookie;
  String csrf;
  MockHttpServletRequestBuilder secure(MockHttpServletRequestBuilder request){return request.cookie(session,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON);}
  @Test void ledgerIsScopedIdempotentAndReversible() throws Exception {
    session=http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"teacher\",\"password\":\"password\"}")).andExpect(status().isOk()).andReturn().getResponse().getCookie("ZJ_SESSION");
    var response=http.perform(get("/api/v1/auth/csrf").cookie(session)).andExpect(status().isOk()).andReturn().getResponse();
    csrfCookie=response.getCookie("XSRF-TOKEN");csrf=json.readTree(response.getContentAsByteArray()).path("data").path("token").asText();
    long before=db.queryForObject("select coalesce(sum(amount),0) from point_ledger where student_id=1",Long.class);
    String body="{\"studentId\":1,\"amount\":7,\"reason\":\"可追溯贡献\",\"idempotencyKey\":\"ledger-verify-1\"}";
    var first=http.perform(secure(post("/api/v1/points")).content(body)).andExpect(status().isOk()).andReturn();
    long id=json.readTree(first.getResponse().getContentAsByteArray()).path("data").path("id").asLong();
    var duplicate=http.perform(secure(post("/api/v1/points")).content(body)).andExpect(status().isOk()).andReturn();
    assertFalse(json.readTree(duplicate.getResponse().getContentAsByteArray()).path("data").path("saved").asBoolean());
    assertEquals(before+7,db.queryForObject("select coalesce(sum(amount),0) from point_ledger where student_id=1",Long.class));
    http.perform(secure(post("/api/v1/points")).content(body.replace("贡献","记录变化"))).andExpect(status().isConflict());
    http.perform(secure(post("/api/v1/points/"+id+"/reverse"))).andExpect(status().isOk());
    var repeat=http.perform(secure(post("/api/v1/points/"+id+"/reverse"))).andExpect(status().isOk()).andReturn();
    assertFalse(json.readTree(repeat.getResponse().getContentAsByteArray()).path("data").path("saved").asBoolean());
    assertEquals(before,db.queryForObject("select coalesce(sum(amount),0) from point_ledger where student_id=1",Long.class));
    long reversal=db.queryForObject("select id from point_ledger where idempotency_key=?",Long.class,"reverse:"+id);
    http.perform(secure(post("/api/v1/points/"+reversal+"/reverse"))).andExpect(status().isConflict());
    var listing=http.perform(get("/api/v1/students/1/points").cookie(session)).andExpect(status().isOk()).andReturn();
    var latest=json.readTree(listing.getResponse().getContentAsByteArray()).path("data").path("items").get(0);
    assertEquals(id,latest.path("reversalOf").asLong()); assertTrue(latest.has("createdAt"));
    http.perform(secure(post("/api/v1/points")).content(body.replace("7","0"))).andExpect(status().isBadRequest());
    String rule="{\"studentId\":1,\"amount\":999,\"ruleId\":1,\"reason\":\"规则验证\",\"idempotencyKey\":\"ledger-rule-1\"}";
    http.perform(secure(post("/api/v1/points")).content(rule)).andExpect(status().isOk());
    assertEquals(before+2,db.queryForObject("select coalesce(sum(amount),0) from point_ledger where student_id=1",Long.class));
    db.update("insert into class_room(id,name,grade,teacher_id,is_demo) values(200,'隔离班级','2026',200,true)");
    db.update("insert into student(id,class_id,student_no,name) values(200,200,'OTHER','其他班级学生')");
    http.perform(get("/api/v1/students/200/points").cookie(session)).andExpect(status().isNotFound());
    http.perform(secure(post("/api/v1/points")).content(body.replace("\"studentId\":1","\"studentId\":200"))).andExpect(status().isNotFound());
    http.perform(get("/api/v1/students/1/points")).andExpect(status().isUnauthorized());
  }
}
