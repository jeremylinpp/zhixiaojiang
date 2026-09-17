package com.zhixiaojiang.model.po;

/** 表 student_message 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class StudentMessage {
    /** 列 id */
    private Long id;

    /** 列 student_id */
    private Long studentId;

    /** 列 event_key */
    private String eventKey;

    /** 列 title */
    private String title;

    /** 列 content */
    private String content;

    /** 列 destination */
    private String destination;

    /** 列 created_at */
    private java.time.LocalDateTime createdAt;

    /** 列 read_at */
    private java.time.LocalDateTime readAt;

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

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(java.time.LocalDateTime readAt) {
        this.readAt = readAt;
    }

}
