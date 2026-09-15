package com.zhixiaojiang.common.constant;

/**
 * 规则引擎的规则编码。
 *
 * <p>每条规则只描述可核实的事实（成绩、迟到、任务、活动参与），
 * 不产出心理或行为结论；结论由教师研判给出。
 */
public enum WarningRule {
    /** 同科目最近四次考试连续下降。 */
    SCORE_DECLINE,
    /** 最近两次考试低于及格线。 */
    SCORE_LOW,
    /** 最近 14 天迟到至少 3 次。 */
    LATE_14D,
    /** 至少 2 项到期任务未完成。 */
    TASK_OVERDUE,
    /** 最近 14 天活动参与较前 14 天下降至少一半。 */
    ACTIVITY_DROP;

    public static WarningRule of(String value) {
        for (WarningRule rule : values()) if (rule.name().equals(value)) return rule;
        return null;
    }
}
