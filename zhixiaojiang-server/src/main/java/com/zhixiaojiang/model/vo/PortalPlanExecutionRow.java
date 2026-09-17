package com.zhixiaojiang.model.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 学生记录的成长计划执行情况。 */
public class PortalPlanExecutionRow {
    private Long id;
    private String content;
    private LocalDate occurredOn;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
