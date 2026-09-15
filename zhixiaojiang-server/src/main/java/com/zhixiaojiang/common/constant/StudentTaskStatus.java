package com.zhixiaojiang.common.constant;

/** 学生任务状态：已指派、已完成。 */
public enum StudentTaskStatus {
    ASSIGNED,
    COMPLETED;

    public static StudentTaskStatus of(String value) {
        for (StudentTaskStatus status : values()) if (status.name().equals(value)) return status;
        return null;
    }
}
