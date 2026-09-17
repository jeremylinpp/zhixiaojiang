package com.zhixiaojiang.model.po;

/** 表 activity_record 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class ActivityRecord {
    /** 列 id */
    private Long id;

    /** 列 student_id */
    private Long studentId;

    /** 列 activity_date */
    private java.time.LocalDate activityDate;

    /** 列 activity_type */
    private String activityType;

    /** 列 status */
    private String status;

    /** 列 detail */
    private String detail;

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

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public java.time.LocalDate getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(java.time.LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
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
