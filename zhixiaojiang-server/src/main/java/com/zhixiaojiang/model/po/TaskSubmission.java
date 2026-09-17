package com.zhixiaojiang.model.po;

/** 表 task_submission 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class TaskSubmission {
    /** 列 id */
    private Long id;

    /** 列 student_task_id */
    private Long studentTaskId;

    /** 列 request_key */
    private String requestKey;

    /** 列 content */
    private String content;

    /** 列 status */
    private String status;

    /** 列 feedback */
    private String feedback;

    /** 列 created_at */
    private java.time.LocalDateTime createdAt;

    /** 列 reviewed_at */
    private java.time.LocalDateTime reviewedAt;

    /** 列 reviewed_by */
    private Long reviewedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentTaskId() {
        return studentTaskId;
    }

    public void setStudentTaskId(Long studentTaskId) {
        this.studentTaskId = studentTaskId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(java.time.LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

}
