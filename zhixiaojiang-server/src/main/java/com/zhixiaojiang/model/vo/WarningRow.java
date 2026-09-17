package com.zhixiaojiang.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 预警列表/详情行：预警本体 + 学生姓名学号，证据列解析为数组。 */
public class WarningRow {
    private Long id;
    private Long studentId;
    private String level;
    private String ruleCode;
    private String summary;
    private String status;
    private String teacherNote;
    private LocalDateTime createdAt;
    private String studentName;
    private String studentNo;
    /** 由 JsonListTypeHandler 从 evidence_json 解析；对外字段名仍是 evidence。 */
    private List<Object> evidence;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTeacherNote() { return teacherNote; }
    public void setTeacherNote(String teacherNote) { this.teacherNote = teacherNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public List<Object> getEvidence() { return evidence; }
    public void setEvidence(List<Object> evidence) { this.evidence = evidence; }
}
