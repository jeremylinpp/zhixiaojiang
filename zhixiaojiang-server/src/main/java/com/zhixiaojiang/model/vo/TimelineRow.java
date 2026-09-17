package com.zhixiaojiang.model.vo;

/** 查询投影：TimelineRow（字段名即 JSON 键）。 */
public class TimelineRow {
    private String title;
    private String detail;
    private java.time.LocalDate occurredOn;
    private String source;
    private Long createdBy;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public java.time.LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(java.time.LocalDate occurredOn) { this.occurredOn = occurredOn; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
