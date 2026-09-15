package com.zhixiaojiang.common.constant;

/** 学生档案状态：在籍与归档，归档后不再出现在在籍列表，但保留全部历史记录。 */
public enum StudentStatus {
    ACTIVE,
    ARCHIVED;

    /** 解析数据库值；未知值返回 null。 */
    public static StudentStatus of(String value) {
        for (StudentStatus status : values()) if (status.name().equals(value)) return status;
        return null;
    }
}
