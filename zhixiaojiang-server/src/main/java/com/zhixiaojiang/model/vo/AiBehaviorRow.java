package com.zhixiaojiang.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 送模型上下文的行为记录行。 */
public class AiBehaviorRow {
    private String category;
    private BigDecimal score;
    private LocalDate occurredOn;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
}
