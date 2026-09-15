package com.zhixiaojiang.common.constant;

import java.util.Set;

/**
 * 一人一策状态机。
 *
 * <p>草稿必须由教师确认后才能进入执行中；已结束的状态不再回流，需要继续帮扶时新建方案。
 */
public enum InterventionStatus {
    DRAFT,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CLOSED;

    /** 当前状态允许流转到的下一个状态；空集合表示不允许再流转。 */
    public Set<InterventionStatus> allowedNext() {
        return switch (this) {
            case DRAFT -> Set.of(CONFIRMED, CLOSED);
            case CONFIRMED -> Set.of(IN_PROGRESS, CLOSED);
            case IN_PROGRESS -> Set.of(COMPLETED, CLOSED);
            default -> Set.of();
        };
    }

    /** 解析数据库值；未知值返回 null，由调用方按非法流转处理。 */
    public static InterventionStatus of(String value) {
        for (InterventionStatus status : values()) if (status.name().equals(value)) return status;
        return null;
    }
}
