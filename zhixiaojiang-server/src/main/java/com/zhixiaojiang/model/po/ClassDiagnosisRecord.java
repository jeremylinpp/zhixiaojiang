package com.zhixiaojiang.model.po;

/** 表 class_diagnosis_record 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class ClassDiagnosisRecord {
    /** 列 id */
    private Long id;

    /** 列 target_id */
    private Long targetId;

    /** 列 measure */
    private String measure;

    /** 列 review_result */
    private String reviewResult;

    /** 列 recorded_on */
    private java.time.LocalDate recordedOn;

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

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public String getReviewResult() {
        return reviewResult;
    }

    public void setReviewResult(String reviewResult) {
        this.reviewResult = reviewResult;
    }

    public java.time.LocalDate getRecordedOn() {
        return recordedOn;
    }

    public void setRecordedOn(java.time.LocalDate recordedOn) {
        this.recordedOn = recordedOn;
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
