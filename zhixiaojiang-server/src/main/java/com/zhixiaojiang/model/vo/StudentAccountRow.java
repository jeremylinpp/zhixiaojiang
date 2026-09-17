package com.zhixiaojiang.model.vo;

/** 学生的账号信息：账号名与是否待改密。 */
public class StudentAccountRow {
    private String username;
    private Boolean mustChangePassword;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Boolean getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
}
