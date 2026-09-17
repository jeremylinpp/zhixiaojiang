package com.zhixiaojiang.model.po;

/** 表 behavior_record 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class BehaviorRecord {
    /** 列 id */
    private Long id;

    /** 列 student_id */
    private Long studentId;

    /** 列 category */
    private String category;

    /** 列 score */
    private java.math.BigDecimal score;

    /** 列 occurred_on */
    private java.time.LocalDate occurredOn;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public java.math.BigDecimal getScore() {
        return score;
    }

    public void setScore(java.math.BigDecimal score) {
        this.score = score;
    }

    public java.time.LocalDate getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(java.time.LocalDate occurredOn) {
        this.occurredOn = occurredOn;
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
