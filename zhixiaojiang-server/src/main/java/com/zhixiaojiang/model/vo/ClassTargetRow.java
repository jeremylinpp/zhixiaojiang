package com.zhixiaojiang.model.vo;

import java.math.BigDecimal;

/** 班级诊改指标行：目标值、当前值与偏差。 */
public class ClassTargetRow {
    private Long id;
    private String name;
    private BigDecimal targetValue;
    private BigDecimal currentValue;
    private String unit;
    private String status;
    private BigDecimal deviation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getDeviation() { return deviation; }
    public void setDeviation(BigDecimal deviation) { this.deviation = deviation; }
}
