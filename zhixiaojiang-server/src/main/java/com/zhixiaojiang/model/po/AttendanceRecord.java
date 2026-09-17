package com.zhixiaojiang.model.po;

/** 表 attendance_record 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class AttendanceRecord {
    /** 列 id */
    private Long id;

    /** 列 student_id */
    private Long studentId;

    /** 列 attendance_date */
    private java.time.LocalDate attendanceDate;

    /** 列 status */
    private String status;

    /** 列 note */
    private String note;

    /** 列 created_by */
    private Long createdBy;

    /** 列 created_at */
    private java.time.LocalDateTime createdAt;

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

    public java.time.LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(java.time.LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

}
