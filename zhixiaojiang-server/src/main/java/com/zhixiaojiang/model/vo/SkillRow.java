package com.zhixiaojiang.model.vo;

/** 查询投影：SkillRow（字段名即 JSON 键）。 */
public class SkillRow {
    private Long id;
    private String skillName;
    private java.math.BigDecimal score;
    private String level;
    private java.time.LocalDate occurredOn;
    private String evidence;
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public java.math.BigDecimal getScore() { return score; }
    public void setScore(java.math.BigDecimal score) { this.score = score; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public java.time.LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(java.time.LocalDate occurredOn) { this.occurredOn = occurredOn; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
