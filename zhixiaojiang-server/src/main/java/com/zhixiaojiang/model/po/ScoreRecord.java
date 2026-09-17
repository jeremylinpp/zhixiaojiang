package com.zhixiaojiang.model.po;

/** 表 score_record 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class ScoreRecord {
    /** 列 id */
    private Long id;

    /** 列 student_id */
    private Long studentId;

    /** 列 subject */
    private String subject;

    /** 列 exam_name */
    private String examName;

    /** 列 score */
    private java.math.BigDecimal score;

    /** 列 full_score */
    private java.math.BigDecimal fullScore;

    /** 列 occurred_on */
    private java.time.LocalDate occurredOn;

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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getExamName() {
        return examName;
    }

    public void setExamName(String examName) {
        this.examName = examName;
    }

    public java.math.BigDecimal getScore() {
        return score;
    }

    public void setScore(java.math.BigDecimal score) {
        this.score = score;
    }

    public java.math.BigDecimal getFullScore() {
        return fullScore;
    }

    public void setFullScore(java.math.BigDecimal fullScore) {
        this.fullScore = fullScore;
    }

    public java.time.LocalDate getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(java.time.LocalDate occurredOn) {
        this.occurredOn = occurredOn;
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
