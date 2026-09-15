package com.zhixiaojiang.common.constant;

/**
 * 机智币流水类别。
 *
 * <p>积分账本只追加不修改：撤销纠错时新增 {@link #REVERSAL} 反向流水，原流水保留。
 */
public enum PointCategory {
    /** 教师手工调整。 */
    MANUAL,
    /** 按积分规则发放。 */
    RULE,
    /** 完成任务自动发放。 */
    TASK,
    /** 撤销原流水生成的反向流水。 */
    REVERSAL;

    public static PointCategory of(String value) {
        for (PointCategory category : values()) if (category.name().equals(value)) return category;
        return null;
    }
}
