package com.zhixiaojiang.common.constant;

/**
 * 预警等级。
 *
 * <p>规则引擎写入 {@link #ATTENTION} 与 {@link #FOCUS}；教师研判时若选择升级，
 * 写入 {@link #MANUAL}。技术设计文档约定的取值是 NORMAL/ATTENTION/FOCUS/REVIEW，
 * 与当前实现的 MANUAL 不一致，统一取值需要产品确认后再改动（含历史数据迁移）。
 */
public enum WarningLevel {
    NORMAL,
    ATTENTION,
    FOCUS,
    REVIEW,
    MANUAL;

    public static WarningLevel of(String value) {
        for (WarningLevel level : values()) if (level.name().equals(value)) return level;
        return null;
    }
}
