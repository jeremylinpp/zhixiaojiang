package com.zhixiaojiang.model.vo;

import java.math.BigDecimal;

/** 某日出勤汇总：已登记条数与出勤率（分母只算已登记记录）。 */
public class AttendanceSummary {
    private Integer registeredCount;
    private BigDecimal attendanceRate;

    public Integer getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(Integer registeredCount) { this.registeredCount = registeredCount; }
    public BigDecimal getAttendanceRate() { return attendanceRate; }
    public void setAttendanceRate(BigDecimal attendanceRate) { this.attendanceRate = attendanceRate; }
}
