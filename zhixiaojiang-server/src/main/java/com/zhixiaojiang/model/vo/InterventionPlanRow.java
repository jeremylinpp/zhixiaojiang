package com.zhixiaojiang.model.vo;

import java.time.LocalDate;

/** 帮扶方案列表行。 */
public class InterventionPlanRow {
    private Long id;
    private Long studentId;
    private String title;
    private String status;
    private String teacherNote;
    private String studentName;
    private LocalDate reviewAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTeacherNote() { return teacherNote; }
    public void setTeacherNote(String teacherNote) { this.teacherNote = teacherNote; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public LocalDate getReviewAt() { return reviewAt; }
    public void setReviewAt(LocalDate reviewAt) { this.reviewAt = reviewAt; }
}
