package com.zhixiaojiang.model.po;

/** 表 class_room 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class ClassRoom {
    /** 列 id */
    private Long id;

    /** 列 name */
    private String name;

    /** 列 grade */
    private String grade;

    /** 列 teacher_id */
    private Long teacherId;

    /** 列 is_demo */
    private Boolean isDemo;

    /** 列 created_at */
    private java.time.LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Boolean getIsDemo() {
        return isDemo;
    }

    public void setIsDemo(Boolean isDemo) {
        this.isDemo = isDemo;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
