package com.zhixiaojiang.model.po;

/** 表 student_growth_revision 的字段载体；由 scripts/generate-po.py 从 schema.sql 生成。 */
public class StudentGrowthRevision {
    /** 列 previous_id */
    private Long previousId;

    /** 列 replacement_id */
    private Long replacementId;

    public Long getPreviousId() {
        return previousId;
    }

    public void setPreviousId(Long previousId) {
        this.previousId = previousId;
    }

    public Long getReplacementId() {
        return replacementId;
    }

    public void setReplacementId(Long replacementId) {
        this.replacementId = replacementId;
    }

}
