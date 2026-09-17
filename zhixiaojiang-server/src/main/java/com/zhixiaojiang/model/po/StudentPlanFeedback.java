package com.zhixiaojiang.model.po;

/** 表 student_plan_feedback 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class StudentPlanFeedback {
    /** 列 id */
    private Long id;

    /** 列 publication_id */
    private Long publicationId;

    /** 列 request_key */
    private String requestKey;

    /** 列 content */
    private String content;

    /** 列 created_by */
    private Long createdBy;

    /** 列 created_at */
    private java.time.LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(Long publicationId) {
        this.publicationId = publicationId;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
