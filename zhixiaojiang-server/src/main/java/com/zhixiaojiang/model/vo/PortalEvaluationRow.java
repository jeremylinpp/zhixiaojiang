package com.zhixiaojiang.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** PortalEvaluationRow 投影。 */
public class PortalEvaluationRow {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal moralScore;
    private BigDecimal skillScore;
    private BigDecimal thinkingScore;
    private BigDecimal smartScore;
    private String evidence;

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public BigDecimal getMoralScore() { return moralScore; }
    public void setMoralScore(BigDecimal moralScore) { this.moralScore = moralScore; }
    public BigDecimal getSkillScore() { return skillScore; }
    public void setSkillScore(BigDecimal skillScore) { this.skillScore = skillScore; }
    public BigDecimal getThinkingScore() { return thinkingScore; }
    public void setThinkingScore(BigDecimal thinkingScore) { this.thinkingScore = thinkingScore; }
    public BigDecimal getSmartScore() { return smartScore; }
    public void setSmartScore(BigDecimal smartScore) { this.smartScore = smartScore; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
}
