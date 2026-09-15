package com.zhixiaojiang.common.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** SQL 动态片段与参数拼装：集中处理 {@code in (?,?)} 与参数追加顺序。 */
public final class SqlParams {
    private SqlParams() {
    }

    /** 生成 {@code in (?,?,...)} 片段；调用方必须按同样顺序传入参数。 */
    public static String inClause(List<Long> ids) {
        return "(" + String.join(",", Collections.nCopies(ids.size(), "?")) + ")";
    }

    /** 在已有参数之后追加参数，保持 JDBC 的绑定顺序。 */
    public static Object[] append(Object[] base, Object... extra) {
        Object[] merged = Arrays.copyOf(base, base.length + extra.length);
        System.arraycopy(extra, 0, merged, base.length, extra.length);
        return merged;
    }
}
