package com.zhixiaojiang.model.vo;

import java.time.LocalDate;

/** 六机成长任务列表行。 */
public class TaskRow {
    private Long id;
    private String module;
    private String title;
    private String description;
    private LocalDate dueOn;
    private Integer pointReward;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDueOn() { return dueOn; }
    public void setDueOn(LocalDate dueOn) { this.dueOn = dueOn; }
    public Integer getPointReward() { return pointReward; }
    public void setPointReward(Integer pointReward) { this.pointReward = pointReward; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
