package com.zhixiaojiang.model.vo;

import java.time.LocalDate;

/** PortalTodoRow 投影。 */
public class PortalTodoRow {
    private Long id;
    private String title;
    private String module;
    private LocalDate dueOn;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public LocalDate getDueOn() { return dueOn; }
    public void setDueOn(LocalDate dueOn) { this.dueOn = dueOn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
