package com.zhixiaojiang.model.vo;

/** 学生档案摘要：详情接口返回的 student 字段。 */
public class StudentSummary {
    private Long id;
    private String studentNo;
    private String name;
    private String gender;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
