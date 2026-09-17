package com.zhixiaojiang.model.vo;

import java.time.LocalDateTime;

/** 预警研判/关闭的过程记录，来自审计表。 */
public class WarningEventRow {
    private Long id;
    private String action;
    private String note;
    private LocalDateTime createdAt;
    private String actor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
}
