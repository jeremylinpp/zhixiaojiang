package com.zhixiaojiang.model.po;

/** 表 student_plan_publication 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class StudentPlanPublication {
    /** 列 id */
    private Long id;

    /** 列 plan_id */
    private Long planId;

    /** 列 version */
    private Integer version;

    /** 列 title */
    private String title;

    /** 列 goal */
    private String goal;

    /** 列 actions */
    private String actions;

    /** 列 review_on */
    private java.time.LocalDate reviewOn;

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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public java.time.LocalDate getReviewOn() {
        return reviewOn;
    }

    public void setReviewOn(java.time.LocalDate reviewOn) {
        this.reviewOn = reviewOn;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
