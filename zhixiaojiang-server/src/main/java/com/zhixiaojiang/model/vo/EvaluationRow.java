package com.zhixiaojiang.model.vo;

/** 查询投影：EvaluationRow（字段名即 JSON 键）。 */
public class EvaluationRow {
    private Long id;
    private java.time.LocalDate periodStart;
    private java.time.LocalDate periodEnd;
    private java.math.BigDecimal moralScore;
    private java.math.BigDecimal skillScore;
    private java.math.BigDecimal thinkingScore;
    private java.math.BigDecimal smartScore;
    private String evidence;
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public java.time.LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(java.time.LocalDate periodStart) { this.periodStart = periodStart; }
    public java.time.LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(java.time.LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public java.math.BigDecimal getMoralScore() { return moralScore; }
    public void setMoralScore(java.math.BigDecimal moralScore) { this.moralScore = moralScore; }
    public java.math.BigDecimal getSkillScore() { return skillScore; }
    public void setSkillScore(java.math.BigDecimal skillScore) { this.skillScore = skillScore; }
    public java.math.BigDecimal getThinkingScore() { return thinkingScore; }
    public void setThinkingScore(java.math.BigDecimal thinkingScore) { this.thinkingScore = thinkingScore; }
    public java.math.BigDecimal getSmartScore() { return smartScore; }
    public void setSmartScore(java.math.BigDecimal smartScore) { this.smartScore = smartScore; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
