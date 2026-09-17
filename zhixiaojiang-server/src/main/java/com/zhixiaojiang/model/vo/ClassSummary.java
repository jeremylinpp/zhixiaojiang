package com.zhixiaojiang.model.vo;

/** 教师负责的班级及其在籍学生数。 */
public class ClassSummary {
    private Long id;
    private String name;
    private String grade;
    private Boolean isDemo;
    private Integer studentCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Boolean getIsDemo() { return isDemo; }
    public void setIsDemo(Boolean isDemo) { this.isDemo = isDemo; }
    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }
}
