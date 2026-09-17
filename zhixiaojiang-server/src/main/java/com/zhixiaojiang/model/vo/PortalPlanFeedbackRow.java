package com.zhixiaojiang.model.vo;

import java.time.LocalDateTime;

/** 教师发布给学生的计划阶段反馈。 */
public class PortalPlanFeedbackRow {
    private Long id;
    private String content;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
