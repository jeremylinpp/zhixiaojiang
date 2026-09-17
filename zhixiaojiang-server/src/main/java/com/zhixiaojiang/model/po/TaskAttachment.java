package com.zhixiaojiang.model.po;

/** 表 task_attachment 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class TaskAttachment {
    /** 列 id */
    private Long id;

    /** 列 student_task_id */
    private Long studentTaskId;

    /** 列 submission_id */
    private Long submissionId;

    /** 列 request_key */
    private String requestKey;

    /** 列 original_name */
    private String originalName;

    /** 列 content_type */
    private String contentType;

    /** 列 size_bytes */
    private Integer sizeBytes;

    /** 列 sha256 */
    private String sha256;

    /** 列 file_bytes */
    private String fileBytes;

    /** 列 created_at */
    private java.time.LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentTaskId() {
        return studentTaskId;
    }

    public void setStudentTaskId(Long studentTaskId) {
        this.studentTaskId = studentTaskId;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Integer sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getFileBytes() {
        return fileBytes;
    }

    public void setFileBytes(String fileBytes) {
        this.fileBytes = fileBytes;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
