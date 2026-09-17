package com.zhixiaojiang.model.vo;

/** 登录时用于签发会话的学生身份信息。 */
public class StudentIdentity {
    private Boolean mustChangePassword;
    private Long sessionVersion;

    public Boolean getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public Long getSessionVersion() { return sessionVersion; }
    public void setSessionVersion(Long sessionVersion) { this.sessionVersion = sessionVersion; }
}
