package com.zhixiaojiang.model.po;

/** 表 student_task 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class StudentTask {
    /** 列 id */
    private Long id;

    /** 列 task_id */
    private Long taskId;

    /** 列 student_id */
    private Long studentId;

    /** 列 status */
    private String status;

    /** 列 completed_on */
    private java.time.LocalDate completedOn;

    /** 列 teacher_note */
    private String teacherNote;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.time.LocalDate getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(java.time.LocalDate completedOn) {
        this.completedOn = completedOn;
    }

    public String getTeacherNote() {
        return teacherNote;
    }

    public void setTeacherNote(String teacherNote) {
        this.teacherNote = teacherNote;
    }

}
