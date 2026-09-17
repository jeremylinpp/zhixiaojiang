package com.zhixiaojiang.model.vo;

import java.time.LocalDate;

/** 送模型上下文的任务行。 */
public class AiTaskRow {
    private String module;
    private String title;
    private String status;
    private LocalDate dueOn;

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getDueOn() { return dueOn; }
    public void setDueOn(LocalDate dueOn) { this.dueOn = dueOn; }
}
