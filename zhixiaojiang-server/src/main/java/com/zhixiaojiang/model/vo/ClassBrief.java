package com.zhixiaojiang.model.vo;

/** 班级简要信息，用于驾驶舱标题等展示。 */
public class ClassBrief {
    private Long id;
    private String name;
    private String grade;

    public ClassBrief() {
    }

    public ClassBrief(Long id, String name, String grade) {
        this.id = id;
        this.name = name;
        this.grade = grade;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}
