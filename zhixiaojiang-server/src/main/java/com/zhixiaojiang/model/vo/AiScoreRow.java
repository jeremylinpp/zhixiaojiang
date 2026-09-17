package com.zhixiaojiang.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 送模型上下文的成绩行（匿名，不含身份信息）。 */
public class AiScoreRow {
    private String subject;
    private String examName;
    private BigDecimal score;
    private BigDecimal fullScore;
    private LocalDate occurredOn;

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
