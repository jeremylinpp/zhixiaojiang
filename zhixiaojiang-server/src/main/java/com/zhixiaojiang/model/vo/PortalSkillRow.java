package com.zhixiaojiang.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** PortalSkillRow 投影。 */
public class PortalSkillRow {
    private Long id;
    private String skillName;
    private BigDecimal score;
    private String level;
    private LocalDate occurredOn;
    private String evidence;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
}
