package com.zhixiaojiang.model.vo;

import java.math.BigDecimal;

/** 周期内四维评价平均分；缺失维度为 null，由调用方决定是否计算成长指数。 */
public class DimensionAverages {
    private BigDecimal moral;
    private BigDecimal skill;
    private BigDecimal thinking;
    private BigDecimal smart;

    public BigDecimal getMoral() { return moral; }
    public void setMoral(BigDecimal moral) { this.moral = moral; }
    public BigDecimal getSkill() { return skill; }
    public void setSkill(BigDecimal skill) { this.skill = skill; }
    public BigDecimal getThinking() { return thinking; }
    public void setThinking(BigDecimal thinking) { this.thinking = thinking; }
    public BigDecimal getSmart() { return smart; }
    public void setSmart(BigDecimal smart) { this.smart = smart; }
}
