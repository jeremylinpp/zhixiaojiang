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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class GrowthWorkspaceTest {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    Cookie session,csrfCookie;String csrf;
    ResultActions save(String endpoint,Object body) throws Exception {
        return http.perform(post("/api/v1/students/1/"+endpoint).cookie(session,csrfCookie).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    JsonNode workspace() throws Exception {
        return json.readTree(http.perform(get("/api/v1/students/1/growth-workspace").cookie(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }
    @Test void averagesMissingDimensionsScoresAndScope() throws Exception {
        session=http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"teacher\",\"password\":\"password\"}")).andExpect(status().isOk()).andReturn().getResponse().getCookie("ZJ_SESSION");
        var response=http.perform(get("/api/v1/auth/csrf").cookie(session)).andExpect(status().isOk()).andReturn().getResponse();
        csrfCookie=response.getCookie("XSRF-TOKEN");csrf=json.readTree(response.getContentAsByteArray()).path("data").path("token").asText();
        String today=LocalDate.now().toString();
        assertTrue(workspace().path("growthIndex").isNull());
        assertTrue(workspace().path("dimensions").path("moral").isNull());
        save("evaluations",Map.of("periodStart",today,"periodEnd",today,"moralScore",80,"evidence","评价依据一")).andExpect(status().isOk());
        assertEquals(80,workspace().path("dimensions").path("moral").asDouble());
        assertTrue(workspace().path("growthIndex").isNull());
        save("evaluations",Map.of("periodStart",today,"periodEnd",today,"skillScore",100,"thinkingScore",60,"evidence","评价依据二")).andExpect(status().isOk());
        assertTrue(workspace().path("growthIndex").isNull());
        save("evaluations",Map.of("periodStart",today,"periodEnd",today,"moralScore",100,"smartScore",40,"evidence","评价依据三")).andExpect(status().isOk());
        assertEquals(72.5,workspace().path("growthIndex").asDouble());
        assertEquals(90,workspace().path("dimensions").path("moral").asDouble());
        save("evaluations",Map.of("periodStart",today,"periodEnd",today,"moralScore",101,"evidence","超出范围")).andExpect(status().isBadRequest());
        save("evaluations",Map.of("periodStart",today,"periodEnd",today,"evidence","缺少分数")).andExpect(status().isBadRequest());
        var exam=Map.of("subject","测试科目","examName","独立批次一","score",90,"fullScore",150,"occurredOn",today);
        save("scores",exam).andExpect(status().isOk());
        save("scores",exam).andExpect(status().isConflict());
        save("scores",Map.of("subject","测试科目","examName","独立批次二","score",151,"fullScore",150,"occurredOn",today)).andExpect(status().isBadRequest());
        var latest=workspace().path("scores").get(0);
        assertEquals("独立批次一",latest.path("examName").asText());assertEquals(150,latest.path("fullScore").asInt());
        save("attendance",Map.of("attendanceDate",today,"status","LATE","note","登记一")).andExpect(status().isOk());
        save("attendance",Map.of("attendanceDate",today,"status","PRESENT","note","核实后更新")).andExpect(status().isOk());
        assertEquals(1,db.queryForObject("select count(*) from attendance_record where student_id=1 and attendance_date=?",Integer.class,today));
        save("attendance",Map.of("attendanceDate",today,"status","UNKNOWN")).andExpect(status().isBadRequest());
        save("skills",Map.of("skillName","实训测试","score",88,"occurredOn",today,"evidence","教师核验")).andExpect(status().isOk());
        save("growth",Map.of("title","成长测试","dimension","SMART","score",90,"occurredOn",today,"source","教师录入")).andExpect(status().isOk());
        assertEquals(72.5,workspace().path("growthIndex").asDouble());
        save("growth",Map.of("title","未来记录","dimension","SMART","score",90,"occurredOn",LocalDate.now().plusDays(1).toString(),"source","教师录入")).andExpect(status().isBadRequest());
        db.update("insert into class_room(id,name,grade,teacher_id,is_demo) values(200,'其他班级','2026',200,true)");
        db.update("insert into student(id,class_id,student_no,name) values(200,200,'OTHER','其他学生')");
        http.perform(get("/api/v1/students/200/growth-workspace").cookie(session)).andExpect(status().isNotFound());
        http.perform(get("/api/v1/students/1/growth-workspace")).andExpect(status().isUnauthorized());
    }
}
