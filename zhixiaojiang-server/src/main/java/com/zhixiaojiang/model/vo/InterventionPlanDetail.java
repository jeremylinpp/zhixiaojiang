package com.zhixiaojiang.model.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 帮扶方案详情：含解析后的建议列表。 */
public class InterventionPlanDetail {
    private Long id;
    private Long studentId;
    private Long warningId;
    private String title;
    private String status;
    private String teacherNote;
    private LocalDate reviewAt;
    private LocalDateTime createdAt;
    private String studentName;
    /** 由 JsonListTypeHandler 从 suggestions_json 解析。 */
    private List<Object> suggestions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getWarningId() { return warningId; }
    public void setWarningId(Long warningId) { this.warningId = warningId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTeacherNote() { return teacherNote; }
    public void setTeacherNote(String teacherNote) { this.teacherNote = teacherNote; }
    public LocalDate getReviewAt() { return reviewAt; }
    public void setReviewAt(LocalDate reviewAt) { this.reviewAt = reviewAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public List<Object> getSuggestions() { return suggestions; }
    public void setSuggestions(List<Object> suggestions) { this.suggestions = suggestions; }
}
