package com.zhixiaojiang.common.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.util.Objects;

/** 自增主键回填：用 GeneratedKeyHolder 取新记录 id，不再依赖 select max(id)。 */
public final class JdbcInsert {
    private JdbcInsert() {
    }

    public static long returningId(JdbcTemplate db, String sql, Object... values) {
        var keys = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            return statement;
        }, keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }
}
