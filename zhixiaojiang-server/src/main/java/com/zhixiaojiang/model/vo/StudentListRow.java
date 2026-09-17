package com.zhixiaojiang.model.vo;

import java.math.BigDecimal;

/** 学生列表行：在籍学生 + 成长记录平均分。 */
public class StudentListRow {
    private Long id;
    private String studentNo;
    private String name;
    private String gender;
    private String status;
    private BigDecimal growthIndex;

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
    public BigDecimal getGrowthIndex() { return growthIndex; }
    public void setGrowthIndex(BigDecimal growthIndex) { this.growthIndex = growthIndex; }
}
