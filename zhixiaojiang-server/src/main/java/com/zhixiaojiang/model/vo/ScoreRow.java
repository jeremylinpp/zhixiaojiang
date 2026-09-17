package com.zhixiaojiang.model.vo;

/** 查询投影：ScoreRow（字段名即 JSON 键）。 */
public class ScoreRow {
    private Long id;
    private String subject;
    private String examName;
    private java.math.BigDecimal score;
    private java.math.BigDecimal fullScore;
    private java.time.LocalDate occurredOn;
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public java.math.BigDecimal getScore() { return score; }
    public void setScore(java.math.BigDecimal score) { this.score = score; }
    public java.math.BigDecimal getFullScore() { return fullScore; }
    public void setFullScore(java.math.BigDecimal fullScore) { this.fullScore = fullScore; }
    public java.time.LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(java.time.LocalDate occurredOn) { this.occurredOn = occurredOn; }
}
