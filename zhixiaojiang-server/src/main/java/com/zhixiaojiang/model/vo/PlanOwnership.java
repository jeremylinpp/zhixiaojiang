package com.zhixiaojiang.model.vo;

/** 帮扶方案归属校验结果：只带状态流转需要的字段。 */
public class PlanOwnership {
    private Long id;
    private Long studentId;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
