package com.zhixiaojiang.auth;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Resolve the student from the authenticated account, never from client-supplied ids. */
@Component
public class StudentScope {
    private final JdbcTemplate db;
    private final CurrentTeacher current;

    public StudentScope(JdbcTemplate db, CurrentTeacher current) {
        this.db = db;
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
        long id = db.queryForList("select s.id from student_account a join student s on s.id=a.student_id "
                        + "join sys_user u on u.id=a.user_id where a.user_id=? and u.role='STUDENT' and s.status='ACTIVE'",
                Long.class, current.id()).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "账号未关联有效学生档案"));
        if(requirePasswordChangeCompleted && Boolean.TRUE.equals(db.queryForObject("select must_change_password from student_account where user_id=?",Boolean.class,current.id())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"首次登录请先修改密码");
        return id;
    }
}
