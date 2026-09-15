package com.zhixiaojiang.controller;

import com.zhixiaojiang.auth.SessionRevocationService;
import com.zhixiaojiang.auth.SessionToken;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.config.SecurityConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 登录、退出与当前教师信息；会话以 HttpOnly Cookie 传递。 */
@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final SessionRevocationService revocations;
    private final TeacherScope scope;
    private final String secret;
    private final boolean secureCookie;

    public AuthController(JdbcTemplate db, PasswordEncoder encoder, SessionRevocationService revocations, TeacherScope scope, org.springframework.core.env.Environment env) {
        this.db = db;
        this.encoder = encoder;
        this.revocations = revocations;
        this.scope = scope;
        this.secret = env.getProperty("app.jwt-secret", "change-me-in-local-env-please-32-chars");
        this.secureCookie = env.getProperty("app.cookie-secure", Boolean.class, false);
    }

    @GetMapping("/auth/csrf")
    Map<String, Object> csrf(CsrfToken token) {
        return ApiResult.ok(Map.of("token", token.getToken()));
    }

    @PostMapping("/auth/login")
    Map<String, Object> login(@Valid @RequestBody Login body, HttpServletResponse response) {
        var rows = db.queryForList("select id,username,password_hash,display_name,role from sys_user where username=?", body.username());
        if (rows.isEmpty() || !encoder.matches(body.password(), (String) rows.get(0).get("password_hash")))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        var u = rows.get(0);
        Cookie c = new Cookie(SecurityConfig.SESSION_COOKIE, SessionToken.issue(((Number) u.get("id")).longValue(), body.username(), (String) u.get("role"), secret));
        c.setHttpOnly(true);
        c.setSecure(secureCookie);
        c.setPath("/");
        c.setMaxAge(28800);
        response.addCookie(c);
        return ApiResult.ok(Map.of("id", u.get("id"), "displayName", u.get("display_name"), "role", u.get("role")));
    }

    @PostMapping("/auth/logout")
    Map<String, Object> logout(HttpServletRequest req, HttpServletResponse r) {
        if (req.getCookies() != null) for (Cookie existing : req.getCookies())
            if (SecurityConfig.SESSION_COOKIE.equals(existing.getName()))
                revocations.revoke(existing.getValue(), java.time.Duration.ofHours(8));
        Cookie c = new Cookie(SecurityConfig.SESSION_COOKIE, "");
        c.setMaxAge(0);
        c.setPath("/");
        r.addCookie(c);
        return ApiResult.ok(Map.of());
    }

    @GetMapping("/auth/me")
    Map<String, Object> me(HttpServletRequest req) {
        long teacher = scope.teacher(req);
        return ApiResult.ok(db.queryForMap("select id,username,display_name as displayName,role from sys_user where id=?", teacher));
    }

    record Login(@NotBlank String username, @NotBlank String password) {
    }
}
