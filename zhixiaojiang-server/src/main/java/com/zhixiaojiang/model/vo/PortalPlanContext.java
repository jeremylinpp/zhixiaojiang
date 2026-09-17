package com.zhixiaojiang.model.vo;

/** 计划版本的归属与状态：学生端按学生校验，教师端按班级校验。 */
public class PortalPlanContext {
    private Long planId;
    private Integer version;
    private Long studentId;
    private String status;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
