package com.zhixiaojiang.model.vo;

import java.time.LocalDateTime;

/** TaskSubmissionRow 投影。 */
public class TaskSubmissionRow {
    private Long id;
    /** 本次提交关联的附件，由服务层填充。 */
    private java.util.List<TaskAttachmentRow> attachments;
    private String content;
    private String status;
    private String feedback;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public java.util.List<TaskAttachmentRow> getAttachments() { return attachments; }
    public void setAttachments(java.util.List<TaskAttachmentRow> attachments) { this.attachments = attachments; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
