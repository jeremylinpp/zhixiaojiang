package com.zhixiaojiang.model.po;

/** 表 warning_record 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class WarningRecord {
    /** 列 id */
    private Long id;

    /** 列 student_id */
    private Long studentId;

    /** 列 level */
    private String level;

    /** 列 rule_code */
    private String ruleCode;

    /** 列 summary */
    private String summary;

    /** 列 evidence_json */
    private String evidenceJson;

    /** 列 status */
    private String status;

    /** 列 teacher_note */
    private String teacherNote;

    /** 列 created_at */
    private java.time.LocalDateTime createdAt;

    /** 列 closed_at */
    private java.time.LocalDateTime closedAt;

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

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTeacherNote() {
        return teacherNote;
    }

    public void setTeacherNote(String teacherNote) {
        this.teacherNote = teacherNote;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(java.time.LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

}
