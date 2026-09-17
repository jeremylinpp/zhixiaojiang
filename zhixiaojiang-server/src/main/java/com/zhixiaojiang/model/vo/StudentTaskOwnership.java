package com.zhixiaojiang.model.vo;

/** 学生任务归属校验结果：任务由当前教师发布，且学生属于其班级。 */
public class StudentTaskOwnership {
    private Long id;
    private Long studentId;
    private String status;
    private Integer pointReward;
    private String title;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPointReward() { return pointReward; }
    public void setPointReward(Integer pointReward) { this.pointReward = pointReward; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
