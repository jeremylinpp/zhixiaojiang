package com.zhixiaojiang.common.constant;

/**
 * 六机成长任务模块：铸机魂、立机规、淬机质、铺机路、聚机力、调机态。
 *
 * <p>数据库存储的是中文模块名，因此枚举携带标签，写入时使用 {@link #label()}。
 */
public enum GrowthModule {
    SOUL("铸机魂"),
    RULE("立机规"),
    QUALITY("淬机质"),
    PATH("铺机路"),
    POWER("聚机力"),
    MOOD("调机态");

    private final String label;

    GrowthModule(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 新建任务未指定模块时的默认模块。 */
    public static String defaultLabel() {
        return POWER.label();
    }

    /** 解析模块中文名；未知值返回 null。 */
    public static GrowthModule ofLabel(String label) {
        for (GrowthModule module : values()) if (module.label.equals(label)) return module;
        return null;
    }
}
