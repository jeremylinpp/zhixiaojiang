package com.zhixiaojiang.model.po;

/** 表 class_target 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class ClassTarget {
    /** 列 id */
    private Long id;

    /** 列 class_id */
    private Long classId;

    /** 列 name */
    private String name;

    /** 列 target_value */
    private java.math.BigDecimal targetValue;

    /** 列 current_value */
    private java.math.BigDecimal currentValue;

    /** 列 unit */
    private String unit;

    /** 列 status */
    private String status;

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

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public java.math.BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(java.math.BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    public java.math.BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(java.math.BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
