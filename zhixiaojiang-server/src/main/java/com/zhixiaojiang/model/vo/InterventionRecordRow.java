package com.zhixiaojiang.model.vo;

import java.time.LocalDate;

/** 帮扶执行过程记录行。 */
public class InterventionRecordRow {
    private Long id;
    private String action;
    private String status;
    private LocalDate occurredOn;
    private String result;
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
