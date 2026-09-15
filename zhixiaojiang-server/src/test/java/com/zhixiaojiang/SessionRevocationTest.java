package com.zhixiaojiang;

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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 退出登录必须立即失效，且不依赖 Redis。
 *
 * <p>demo profile 关闭了 Redis（app.redis-enabled=false），因此本测试证明：Redis 不可用时
 * 「退出登录」仍然生效，而不是被静默忽略。
 */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo")
class SessionRevocationTest {
  @Autowired MockMvc http;
  @Autowired ObjectMapper json;
  @Autowired SessionRevocationService revocations;

  private Cookie login() throws Exception {
    var response = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"teacher\",\"password\":\"password\"}"))
        .andExpect(status().isOk()).andReturn().getResponse();
    Cookie session = response.getCookie("ZJ_SESSION");
    assertNotNull(session, "登录未返回会话 Cookie");
    return session;
  }

  private MockHttpServletRequestBuilder signed(MockHttpServletRequestBuilder request, Cookie session) throws Exception {
    var csrfResponse = http.perform(get("/api/v1/auth/csrf").cookie(session)).andExpect(status().isOk()).andReturn().getResponse();
    return request.cookie(session, csrfResponse.getCookie("XSRF-TOKEN"))
        .header("X-CSRF-TOKEN", json.readTree(csrfResponse.getContentAsByteArray()).path("data").path("token").asText());
  }

  @Test
  void logoutRevokesTokenWithoutRedis() throws Exception {
    Cookie session = login();
    http.perform(get("/api/v1/students/1").cookie(session)).andExpect(status().isOk());
    assertFalse(revocations.isRevoked(session.getValue()));

    http.perform(signed(post("/api/v1/auth/logout"), session)).andExpect(status().isOk());

    assertTrue(revocations.isRevoked(session.getValue()), "退出后本实例必须记住该令牌已撤销");
    http.perform(get("/api/v1/students/1").cookie(session)).andExpect(status().isUnauthorized());
    http.perform(get("/api/v1/dashboard/class").cookie(session)).andExpect(status().isUnauthorized());

    // 同一秒内重新登录得到的令牌必须与已撤销的令牌不同，否则新会话会被误判为已撤销
    Cookie again = login();
    assertNotEquals(session.getValue(), again.getValue());
    http.perform(get("/api/v1/students/1").cookie(again)).andExpect(status().isOk());
  }

  @Test
  void markersExpireAndUnrelatedTokensStayValid() throws Exception {
    revocations.revoke("token-under-test", Duration.ofMillis(50));
    assertTrue(revocations.isRevoked("token-under-test"));
    Thread.sleep(120);
    assertFalse(revocations.isRevoked("token-under-test"), "到期后标记应自动失效");

    revocations.revoke("", Duration.ofHours(8));
    revocations.revoke(null, Duration.ofHours(8));
    revocations.revoke("no-ttl", Duration.ZERO);
    assertFalse(revocations.isRevoked(""));
    assertFalse(revocations.isRevoked(null));
    assertFalse(revocations.isRevoked("no-ttl"), "零有效期不应产生标记");

    Cookie session = login();
    http.perform(get("/api/v1/students/1").cookie(session)).andExpect(status().isOk());
  }
}
