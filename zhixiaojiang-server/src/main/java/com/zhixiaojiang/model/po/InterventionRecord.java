package com.zhixiaojiang.model.po;

/** 表 intervention_record 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class InterventionRecord {
    /** 列 id */
    private Long id;

    /** 列 plan_id */
    private Long planId;

    /** 列 action */
    private String action;

    /** 列 status */
    private String status;

    /** 列 occurred_on */
    private java.time.LocalDate occurredOn;

    /** 列 result */
    private String result;

    /** 列 created_by */
    private Long createdBy;

    /** 列 created_at */
    private java.time.LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.time.LocalDate getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(java.time.LocalDate occurredOn) {
        this.occurredOn = occurredOn;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
