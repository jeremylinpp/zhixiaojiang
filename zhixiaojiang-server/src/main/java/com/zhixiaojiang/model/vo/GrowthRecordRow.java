package com.zhixiaojiang.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 成长记录行（五类录入之一）。 */
public class GrowthRecordRow {
    private Long id;
    private String dimension;
    private BigDecimal score;
    private String title;
    private String detail;
    private LocalDate occurredOn;
    private String source;
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
