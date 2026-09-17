package com.zhixiaojiang.model.vo;

import java.time.LocalDate;

/** PortalActivityRow 投影。 */
public class PortalActivityRow {
    private Long id;
    private LocalDate activityDate;
    private String activityType;
    private String detail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate activityDate) { this.activityDate = activityDate; }
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
