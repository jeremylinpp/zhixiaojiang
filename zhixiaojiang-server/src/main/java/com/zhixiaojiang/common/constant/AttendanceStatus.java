package com.zhixiaojiang.common.constant;

/** 每日出勤状态。 */
public enum AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT,
    LEAVE;

    /** 供请求参数校验注解使用的正则，必须与枚举常量保持一致。 */
    public static final String PATTERN = "PRESENT|LATE|ABSENT|LEAVE";

    public static AttendanceStatus of(String value) {
        for (AttendanceStatus status : values()) if (status.name().equals(value)) return status;
        return null;
    }
}
