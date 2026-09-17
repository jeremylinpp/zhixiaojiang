package com.zhixiaojiang.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;

/** PortalGrowthRecordRow 投影。 */
public class PortalGrowthRecordRow {
    private Long id;
    /** 仅审核流程在 Java 侧使用（原 select r.* 带入），不对外暴露。 */
    @JsonIgnore
    private Long studentId;
    private String category;
    private String title;
    private String content;
    private LocalDate occurredOn;
    private String status;
    private String feedback;
    private Long previousId;
    private Long replacedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public Long getPreviousId() { return previousId; }
    public void setPreviousId(Long previousId) { this.previousId = previousId; }
    public Long getReplacedBy() { return replacedBy; }
    public void setReplacedBy(Long replacedBy) { this.replacedBy = replacedBy; }
}
