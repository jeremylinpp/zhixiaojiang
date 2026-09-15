package com.zhixiaojiang.common.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 请求体取值与类型转换。
 *
 * <p>集中定义「缺省」「空白」的语义：文本缺省用默认值，数值缺省为 0，
 * 日期缺省分两种——业务必填日期取今天，部分更新日期保持原值（null）。
 */
public final class RequestValues {
    private RequestValues() {
    }

    /** 取字符串；缺省或空白时返回默认值。 */
    public static String text(Map<String, Object> body, String key, String fallback) {
        Object value = body.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    /** 取可空长整型；缺省或空白时为 null。 */
    public static Long longOrNull(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : Long.parseLong(String.valueOf(value));
    }

    /** 取整型；缺省为 0。 */
    public static int intValue(Object value) {
        return value == null ? 0 : Integer.parseInt(String.valueOf(value));
    }

    /** 业务必填日期：缺省取今天。 */
    public static LocalDate date(Object value) {
        return value == null || String.valueOf(value).isBlank() ? LocalDate.now() : LocalDate.parse(String.valueOf(value));
    }

    /** 部分更新日期：缺省返回 null，由 SQL 的 coalesce 保留原值。 */
    public static LocalDate dateOrNull(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : LocalDate.parse(String.valueOf(value));
    }

    /** 取小数；缺省为 0。 */
    public static BigDecimal decimal(Object value) {
        return value == null || String.valueOf(value).isBlank() ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }

    /** 取可空小数。 */
    public static BigDecimal decimalOrNull(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : new BigDecimal(String.valueOf(value));
    }
}
