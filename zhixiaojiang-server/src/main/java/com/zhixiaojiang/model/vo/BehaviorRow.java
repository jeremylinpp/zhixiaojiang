package com.zhixiaojiang.model.vo;

/** 查询投影：BehaviorRow（字段名即 JSON 键）。 */
public class BehaviorRow {
    private Long id;
    private String category;
    private java.math.BigDecimal score;
    private java.time.LocalDate occurredOn;
    private String detail;
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public java.math.BigDecimal getScore() { return score; }
    public void setScore(java.math.BigDecimal score) { this.score = score; }
    public java.time.LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(java.time.LocalDate occurredOn) { this.occurredOn = occurredOn; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
