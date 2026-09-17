package com.zhixiaojiang.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/** 账号信息：查询结果直接对应登录与个人资料两处响应。 */
public class UserAccount {
    private Long id;
    private String username;
    private String displayName;
    private String role;
    /** 口令哈希只供登录校验使用，任何情况下都不序列化到响应里。 */
    @JsonIgnore
    private String passwordHash;
    /** 学生账号首次登录需改密；教师账号该键不出现。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean mustChangePassword;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Boolean getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
}
