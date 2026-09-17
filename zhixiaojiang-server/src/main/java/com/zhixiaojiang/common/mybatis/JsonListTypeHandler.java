package com.zhixiaojiang.common.mybatis;

import com.zhixiaojiang.common.util.JsonValues;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * JSON 数组列 → {@code List<Object>}。
 *
 * <p>只在映射文件里按需声明（{@code typeHandler=...}），不做全局注册：全局注册会把所有 List 属性
 * 都当成 JSON 列解析。解析失败由 {@link JsonValues#toList} 兜底为空列表，不中断业务查询。
 */
@MappedTypes(List.class)
public class JsonListTypeHandler extends BaseTypeHandler<List<Object>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Object> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, JsonValues.toJson(parameter));
    }

    @Override
    public List<Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return JsonValues.toList(rs.getString(columnName));
    }

    @Override
    public List<Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return JsonValues.toList(rs.getString(columnIndex));
    }

    @Override
    public List<Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return JsonValues.toList(cs.getString(columnIndex));
    }
}
