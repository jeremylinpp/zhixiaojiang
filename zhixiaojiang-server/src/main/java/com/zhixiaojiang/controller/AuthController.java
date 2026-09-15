package com.zhixiaojiang.controller;

import com.zhixiaojiang.auth.SessionToken;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.config.SecurityConfig;
import com.zhixiaojiang.model.dto.LoginRequest;
import com.zhixiaojiang.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 登录、退出与当前教师信息；会话以 HttpOnly Cookie 传递，Cookie 细节属于 Web 层。 */
@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService auth;
    private final boolean secureCookie;

    public AuthController(AuthService auth, org.springframework.core.env.Environment env) {
        this.auth = auth;
        this.secureCookie = env.getProperty("app.cookie-secure", Boolean.class, false);
    }

    @GetMapping("/auth/csrf")
    Map<String, Object> csrf(CsrfToken token) {
        return ApiResult.ok(Map.of("token", token.getToken()));
    }

    @PostMapping("/auth/login")
    Map<String, Object> login(@Valid @RequestBody LoginRequest body, HttpServletResponse response) {
        var result = auth.login(body);
        Cookie cookie = new Cookie(SecurityConfig.SESSION_COOKIE, result.token());
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge((int) SessionToken.TTL_SECONDS);
        response.addCookie(cookie);
        return ApiResult.ok(result.teacher());
    }

    @PostMapping("/auth/logout")
    Map<String, Object> logout(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) for (Cookie existing : request.getCookies())
            if (SecurityConfig.SESSION_COOKIE.equals(existing.getName())) auth.logout(existing.getValue());
        Cookie cleared = new Cookie(SecurityConfig.SESSION_COOKIE, "");
        cleared.setMaxAge(0);
        cleared.setPath("/");
        response.addCookie(cleared);
        return ApiResult.ok(Map.of());
    }

    @GetMapping("/auth/me")
    Map<String, Object> me() {
        return ApiResult.ok(auth.currentTeacher());
    }
}
