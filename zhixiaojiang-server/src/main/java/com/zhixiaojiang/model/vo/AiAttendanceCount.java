package com.zhixiaojiang.model.vo;

/** 送模型上下文的出勤分布（各状态各多少次）。 */
public class AiAttendanceCount {
    private String status;
    private Integer total;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
}
