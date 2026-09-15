package com.zhixiaojiang.common.constant;

/** 四维成长维度：品德、技能、思维、智行。 */
public enum GrowthDimension {
    MORAL,
    SKILL,
    THINKING,
    SMART;

    /** 供请求参数校验注解使用的正则，必须与枚举常量保持一致。 */
    public static final String PATTERN = "MORAL|SKILL|THINKING|SMART";

    public static GrowthDimension of(String value) {
        for (GrowthDimension dimension : values()) if (dimension.name().equals(value)) return dimension;
        return null;
    }
}
