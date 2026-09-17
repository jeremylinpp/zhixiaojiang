package com.zhixiaojiang.model.po;

/** 表 growth_task 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class GrowthTask {
    /** 列 id */
    private Long id;

    /** 列 module */
    private String module;

    /** 列 title */
    private String title;

    /** 列 description */
    private String description;

    /** 列 due_on */
    private java.time.LocalDate dueOn;

    /** 列 point_reward */
    private Integer pointReward;

    /** 列 status */
    private String status;

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

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.time.LocalDate getDueOn() {
        return dueOn;
    }

    public void setDueOn(java.time.LocalDate dueOn) {
        this.dueOn = dueOn;
    }

    public Integer getPointReward() {
        return pointReward;
    }

    public void setPointReward(Integer pointReward) {
        this.pointReward = pointReward;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
