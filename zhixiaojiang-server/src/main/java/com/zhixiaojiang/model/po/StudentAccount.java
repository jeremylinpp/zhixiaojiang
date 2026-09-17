package com.zhixiaojiang.model.po;

/** 表 student_account 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class StudentAccount {
    /** 列 user_id */
    private Long userId;

    /** 列 student_id */
    private Long studentId;

    /** 列 must_change_password */
    private Boolean mustChangePassword;

    /** 列 session_version */
    private Long sessionVersion;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public Long getSessionVersion() {
        return sessionVersion;
    }

    public void setSessionVersion(Long sessionVersion) {
        this.sessionVersion = sessionVersion;
    }

}
