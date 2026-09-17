package com.zhixiaojiang.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** PortalScoreRow 投影。 */
public class PortalScoreRow {
    private Long id;
    private String subject;
    private String examName;
    private BigDecimal score;
    private BigDecimal fullScore;
    private LocalDate occurredOn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public BigDecimal getFullScore() { return fullScore; }
    public void setFullScore(BigDecimal fullScore) { this.fullScore = fullScore; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
}
