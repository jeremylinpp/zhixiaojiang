package com.zhixiaojiang.model.po;

/** 表 intervention_plan 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class InterventionPlan {
    /** 列 id */
    private Long id;

    /** 列 student_id */
    private Long studentId;

    /** 列 warning_id */
    private Long warningId;

    /** 列 title */
    private String title;

    /** 列 status */
    private String status;

    /** 列 suggestions_json */
    private String suggestionsJson;

    /** 列 teacher_note */
    private String teacherNote;

    /** 列 review_at */
    private java.time.LocalDate reviewAt;

    /** 列 created_by */
    private Long createdBy;

    /** 列 created_at */
    private java.time.LocalDateTime createdAt;

    /** 列 updated_at */
    private java.time.LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getWarningId() {
        return warningId;
    }

    public void setWarningId(Long warningId) {
        this.warningId = warningId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSuggestionsJson() {
        return suggestionsJson;
    }

    public void setSuggestionsJson(String suggestionsJson) {
        this.suggestionsJson = suggestionsJson;
    }

    public String getTeacherNote() {
        return teacherNote;
    }

    public void setTeacherNote(String teacherNote) {
        this.teacherNote = teacherNote;
    }

    public java.time.LocalDate getReviewAt() {
        return reviewAt;
    }

    public void setReviewAt(java.time.LocalDate reviewAt) {
        this.reviewAt = reviewAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
