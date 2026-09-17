package com.zhixiaojiang.model.vo;

/** 成长工作台标题用的学生简要信息。 */
public class StudentBrief {
    private Long id;
    private String name;
    private String studentNo;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
