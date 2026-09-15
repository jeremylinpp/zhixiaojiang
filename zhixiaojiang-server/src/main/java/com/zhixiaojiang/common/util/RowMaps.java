package com.zhixiaojiang.common.util;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 结果集到 Map 的统一映射：列名转 camelCase，日期与时间转字符串。
 *
 * <p>DAO 一律不使用 camelCase 的 SQL 别名：H2（demo profile）会把未加引号的别名折叠成小写，
 * 而 MySQL 保留大小写，直接依赖别名会让两套数据库返回不同的 JSON 键。改为在 SQL 中只写库内
 * 真实列名（snake_case），由这里统一转成前端使用的 camelCase，两套数据库结果一致。
 */
public final class RowMaps {
    private RowMaps() {
    }

    /** 单行或多行查询的映射器。 */
    public static RowMapper<Map<String, Object>> mapper() {
        return (rs, rowNum) -> of(rs);
    }

    /** 把当前行转成 camelCase 键的 Map；时间戳与日期保持原有的字符串格式。 */
    public static Map<String, Object> of(ResultSet rs) throws SQLException {
        var metadata = rs.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
            row.put(camel(metadata.getColumnLabel(column)), value(rs.getObject(column)));
        }
        return row;
    }

    /**
     * 列名转 camelCase：{@code student_no → studentNo}。
     *
     * <p>要求 SQL 只使用 snake_case 列名或别名，出现 camelCase 别名时这里无法还原大小写。
     */
    public static String camel(String label) {
        String[] words = label.toLowerCase(Locale.ROOT).split("_");
        StringBuilder key = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            if (words[i].isEmpty()) continue;
            key.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
        }
        return key.toString();
    }

    private static Object value(Object raw) {
        if (raw instanceof java.sql.Timestamp timestamp) return timestamp.toString();
        if (raw instanceof java.sql.Date date) return date.toString();
        if (raw instanceof java.time.LocalDate || raw instanceof java.time.LocalDateTime)
            return raw.toString().replace('T', ' ');
        return raw;
    }
}
