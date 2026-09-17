package com.zhixiaojiang.auth;

import com.zhixiaojiang.dao.StudentAccountMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Resolve the student from the authenticated account, never from client-supplied ids. */
@Component
public class StudentScope {
    private final StudentAccountMapper accounts;
    private final CurrentTeacher current;

    public StudentScope(StudentAccountMapper accounts, CurrentTeacher current) {
        this.accounts = accounts;
        this.current = current;
    }

    public long studentId() {
        return studentId(true);
    }

    public long studentId(boolean requirePasswordChangeCompleted) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> "ROLE_STUDENT".equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅学生账号可访问");
        long id = accounts.activeStudentIdOfUser(current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "账号未关联有效学生档案"));
        if (requirePasswordChangeCompleted && Boolean.TRUE.equals(accounts.mustChangePassword(current.id()).orElse(false)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"首次登录请先修改密码");
        return id;
    }
}
