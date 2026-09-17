package com.zhixiaojiang.model.vo;

/** 确认完成时发放奖励所需的信息：学生、奖励分值、任务名。 */
public class TaskReward {
    private Long studentId;
    private Integer pointReward;
    private String title;

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Integer getPointReward() { return pointReward; }
    public void setPointReward(Integer pointReward) { this.pointReward = pointReward; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
