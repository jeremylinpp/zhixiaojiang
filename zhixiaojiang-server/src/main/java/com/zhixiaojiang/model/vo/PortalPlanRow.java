package com.zhixiaojiang.model.vo;

import java.time.LocalDate;

/** 学生可见的成长计划版本行（含方案状态）。 */
public class PortalPlanRow {
    private Long id;
    private String title;
    private String goal;
    private String actions;
    private LocalDate reviewOn;
    private Integer version;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getActions() { return actions; }
    public void setActions(String actions) { this.actions = actions; }
    public LocalDate getReviewOn() { return reviewOn; }
    public void setReviewOn(LocalDate reviewOn) { this.reviewOn = reviewOn; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
