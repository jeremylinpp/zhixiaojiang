package com.zhixiaojiang.common.constant;

/** 预警处理状态：待研判、已研判、已关闭。 */
public enum WarningStatus {
    OPEN,
    REVIEWED,
    CLOSED;

    /** 研判请求允许的起始状态正则，供请求参数校验注解使用。 */
    public static final String PATTERN_TRIAGE = "OPEN|REVIEWED";

    public static WarningStatus of(String value) {
        for (WarningStatus status : values()) if (status.name().equals(value)) return status;
        return null;
    }
}
