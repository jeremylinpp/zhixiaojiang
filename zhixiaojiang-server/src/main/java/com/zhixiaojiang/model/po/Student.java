package com.zhixiaojiang.model.po;

/** 表 student 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class Student {
    /** 列 id */
    private Long id;

    /** 列 class_id */
    private Long classId;

    /** 列 student_no */
    private String studentNo;

    /** 列 name */
    private String name;

    /** 列 gender */
    private String gender;

    /** 列 status */
    private String status;

    /** 列 archived_at */
    private java.time.LocalDateTime archivedAt;

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

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.time.LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(java.time.LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
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
