package com.zhixiaojiang.model.vo;

/** 驾驶舱用的待研判预警摘要。 */
public class WarningBrief {
    private Long id;
    private String level;
    private String summary;
    private String studentName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
}
