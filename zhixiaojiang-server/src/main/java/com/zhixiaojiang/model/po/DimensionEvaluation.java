package com.zhixiaojiang.model.po;

/** 表 dimension_evaluation 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class DimensionEvaluation {
    /** 列 id */
    private Long id;

    /** 列 student_id */
    private Long studentId;

    /** 列 period_start */
    private java.time.LocalDate periodStart;

    /** 列 period_end */
    private java.time.LocalDate periodEnd;

    /** 列 moral_score */
    private java.math.BigDecimal moralScore;

    /** 列 skill_score */
    private java.math.BigDecimal skillScore;

    /** 列 thinking_score */
    private java.math.BigDecimal thinkingScore;

    /** 列 smart_score */
    private java.math.BigDecimal smartScore;

    /** 列 evidence */
    private String evidence;

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

    public java.time.LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(java.time.LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public java.time.LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(java.time.LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public java.math.BigDecimal getMoralScore() {
        return moralScore;
    }

    public void setMoralScore(java.math.BigDecimal moralScore) {
        this.moralScore = moralScore;
    }

    public java.math.BigDecimal getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(java.math.BigDecimal skillScore) {
        this.skillScore = skillScore;
    }

    public java.math.BigDecimal getThinkingScore() {
        return thinkingScore;
    }

    public void setThinkingScore(java.math.BigDecimal thinkingScore) {
        this.thinkingScore = thinkingScore;
    }

    public java.math.BigDecimal getSmartScore() {
        return smartScore;
    }

    public void setSmartScore(java.math.BigDecimal smartScore) {
        this.smartScore = smartScore;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
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
