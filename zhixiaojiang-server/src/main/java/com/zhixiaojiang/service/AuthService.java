package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.SessionRevocationService;
import com.zhixiaojiang.auth.SessionToken;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.dao.StudentAccountMapper;
import com.zhixiaojiang.dao.UserMapper;
import com.zhixiaojiang.model.vo.StudentIdentity;
import com.zhixiaojiang.model.vo.UserAccount;
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
    private final UserMapper users;
    private final PasswordEncoder encoder;
    private final SessionRevocationService revocations;
    private final TeacherScope scope;
    private final String secret;
    private final StudentAccountMapper accounts;

    public AuthService(UserMapper users, StudentAccountMapper accounts, PasswordEncoder encoder, SessionRevocationService revocations, TeacherScope scope, org.springframework.core.env.Environment env) {
        this.users = users;
        this.accounts = accounts;
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
        if (!encoder.matches(request.password(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        long id = user.getId();
        var student = "STUDENT".equals(user.getRole()) ? studentIdentity(id) : java.util.Optional.<StudentIdentity>empty();
        String token = SessionToken.issue(id, request.username(), user.getRole(), secret, student.map(StudentIdentity::getSessionVersion).orElse(0L));
        Map<String, Object> teacher = new LinkedHashMap<>();
        teacher.put("id", id);
        teacher.put("displayName", user.getDisplayName());
        teacher.put("role", user.getRole());
        student.ifPresent(identity -> teacher.put("mustChangePassword", identity.getMustChangePassword()));
        return new LoginResult(token, teacher);
    }

    /** 退出登录：把令牌写入撤销标记，跨实例由 Redis 共享（不可用时本实例仍生效）。 */
    public void logout(String token) {
        revocations.revoke(token, Duration.ofHours(8));
    }

    public UserAccount currentTeacher() {
        UserAccount user = users.findById(scope.teacher())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号已失效"));
        if ("STUDENT".equals(user.getRole()))
            user.setMustChangePassword(studentIdentity(user.getId()).map(StudentIdentity::getMustChangePassword).orElse(null));
        return user;
    }

    private java.util.Optional<StudentIdentity> studentIdentity(long userId) {
        return accounts.identityOfUser(userId);
    }
}
