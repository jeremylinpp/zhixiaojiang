package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

/** 教师账号数据访问。 */
@Repository
public class UserDao {
    private final JdbcTemplate db;

    public UserDao(JdbcTemplate db) {
        this.db = db;
    }

    /** 含口令哈希，仅供登录校验使用，不得直接返回给前端。 */
    public Optional<Map<String, Object>> findByUsername(String username) {
        return db.query("select id,username,password_hash,display_name,role from sys_user where username=?", RowMaps.mapper(), username).stream().findFirst();
    }

    public Optional<Map<String, Object>> findById(long id) {
        return db.query("select id,username,display_name,role from sys_user where id=?", RowMaps.mapper(), id).stream().findFirst();
    }

    /** 按旧值校验后更新显示名称，返回受影响行数（0 表示已被他人修改）。 */
    public int updateDisplayName(long id, String displayName, String previousDisplayName) {
        return db.update("update sys_user set display_name=? where id=? and display_name=?", displayName, id, previousDisplayName);
    }
}
