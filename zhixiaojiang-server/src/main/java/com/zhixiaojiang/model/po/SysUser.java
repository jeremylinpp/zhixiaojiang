package com.zhixiaojiang.model.po;

/** 表 sys_user 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class SysUser {
    /** 列 id */
    private Long id;

    /** 列 username */
    private String username;

    /** 列 password_hash */
    private String passwordHash;

    /** 列 display_name */
    private String displayName;

    /** 列 role */
    private String role;

    /** 列 created_at */
    private java.time.LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
