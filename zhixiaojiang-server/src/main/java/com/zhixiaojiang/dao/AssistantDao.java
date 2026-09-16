package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.util.RowMaps;
import com.zhixiaojiang.common.util.SqlParams;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 助手页所需的工作量统计：已录入的数据量与最近一次规则筛查时间。 */
@Repository
public class AssistantDao {
    private final JdbcTemplate db;

    public AssistantDao(JdbcTemplate db) {
        this.db = db;
    }

    public int countStudents(List<Long> classIds) {
        Integer count = db.queryForObject("select count(*) from student where class_id in " + SqlParams.inClause(classIds) + " and status='ACTIVE'", Integer.class, classIds.toArray());
        return count == null ? 0 : count;
    }

    /** 指定表的记录数，限定在当前教师班级的学生范围内。 */
    public int countForStudents(List<Long> classIds, String table) {
        Integer count = db.queryForObject("select count(*) from " + table + " t join student s on s.id=t.student_id where s.class_id in " + SqlParams.inClause(classIds), Integer.class, classIds.toArray());
        return count == null ? 0 : count;
    }

    /** 最近一次规则筛查时间，取自审计记录。 */
    public String lastRuleAnalysisAt(long teacherId) {
        var rows = db.query("select max(created_at) created_at from audit_log where action='ANALYZE' and actor_id=?", RowMaps.mapper(), teacherId);
        Object value = rows.isEmpty() ? null : rows.get(0).get("createdAt");
        return value == null ? null : value.toString();
    }
}
