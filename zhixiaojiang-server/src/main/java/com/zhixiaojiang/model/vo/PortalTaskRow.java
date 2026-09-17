package com.zhixiaojiang.model.vo;

import java.time.LocalDate;

/** PortalTaskRow 投影。 */
public class PortalTaskRow {
    private Long id;
    private String status;
    private LocalDate completedOn;
    private String teacherNote;
    private String title;
    private String description;
    private String module;
    private LocalDate dueOn;
    private Integer pointReward;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getCompletedOn() { return completedOn; }
    public void setCompletedOn(LocalDate completedOn) { this.completedOn = completedOn; }
    public String getTeacherNote() { return teacherNote; }
    public void setTeacherNote(String teacherNote) { this.teacherNote = teacherNote; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public LocalDate getDueOn() { return dueOn; }
    public void setDueOn(LocalDate dueOn) { this.dueOn = dueOn; }
    public Integer getPointReward() { return pointReward; }
    public void setPointReward(Integer pointReward) { this.pointReward = pointReward; }
}
