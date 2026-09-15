package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.util.JdbcInsert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** AI 分析记录的数据访问：保留送模型的字段白名单与返回内容，供教师追溯。 */
@Repository
public class AnalysisDao {
    private final JdbcTemplate db;

    public AnalysisDao(JdbcTemplate db) {
        this.db = db;
    }

    public long insert(long studentId, String source, String requestJson, String responseJson, long actor) {
        return JdbcInsert.returningId(db, "insert into ai_analysis(student_id,source,request_json,response_json,created_by) values(?,?,?,?,?)",
                studentId, source, requestJson, responseJson, actor);
    }
}
