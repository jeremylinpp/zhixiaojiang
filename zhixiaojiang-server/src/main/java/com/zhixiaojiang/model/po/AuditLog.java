package com.zhixiaojiang.model.po;

/** 表 audit_log 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class AuditLog {
    /** 列 id */
    private Long id;

    /** 列 actor_id */
    private Long actorId;

    /** 列 action */
    private String action;

    /** 列 entity_type */
    private String entityType;

    /** 列 entity_id */
    private Long entityId;

    /** 列 summary */
    private String summary;

    /** 列 created_at */
    private java.time.LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
