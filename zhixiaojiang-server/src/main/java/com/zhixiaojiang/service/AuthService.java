package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.SessionRevocationService;
import com.zhixiaojiang.auth.SessionToken;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.dao.UserDao;
import com.zhixiaojiang.model.dto.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** 登录、退出与当前教师信息。 */
@Service
public class AuthService {
    private final UserDao users;
    private final PasswordEncoder encoder;
    private final SessionRevocationService revocations;
    private final TeacherScope scope;
    private final String secret;

    public AuthService(UserDao users, PasswordEncoder encoder, SessionRevocationService revocations, TeacherScope scope, org.springframework.core.env.Environment env) {
        this.users = users;
        this.encoder = encoder;
        this.revocations = revocations;
        this.scope = scope;
        this.secret = env.getProperty("app.jwt-secret", "change-me-in-local-env-please-32-chars");
    }

    /** 登录结果：会话令牌与用于响应的教师信息（不含口令哈希）。 */
    public record LoginResult(String token, Map<String, Object> teacher) {
    }

    public LoginResult login(LoginRequest request) {
        var user = users.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!encoder.matches(request.password(), String.valueOf(user.get("passwordHash"))))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        long id = ((Number) user.get("id")).longValue();
        String token = SessionToken.issue(id, request.username(), String.valueOf(user.get("role")), secret);
        Map<String, Object> teacher = new LinkedHashMap<>();
        teacher.put("id", id);
        teacher.put("displayName", user.get("displayName"));
        teacher.put("role", user.get("role"));
        return new LoginResult(token, teacher);
    }

    /** 退出登录：把令牌写入撤销标记，跨实例由 Redis 共享（不可用时本实例仍生效）。 */
    public void logout(String token) {
        revocations.revoke(token, Duration.ofHours(8));
    }

    public Map<String, Object> currentTeacher() {
        return users.findById(scope.teacher())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号已失效"));
    }
}
