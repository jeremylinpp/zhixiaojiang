package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 组装送模型的分析上下文。
 *
 * <p>只取可核实的事实类字段（成绩、出勤、行为、技能、任务），不含姓名、学号、联系方式等身份信息；
 * 由服务端按学生 id 查询，不接受前端提交的分析内容。
 */
@Repository
public class AnalysisContextDao {
    private final JdbcTemplate db;

    public AnalysisContextDao(JdbcTemplate db) {
        this.db = db;
    }

    public List<Map<String, Object>> scores(long studentId) {
        return db.query("select subject,exam_name,score,full_score,occurred_on from score_record where student_id=? order by occurred_on desc limit 20", RowMaps.mapper(), studentId);
    }

    /** 最近一段时间的出勤次数分布，例如各状态各多少次。 */
    public List<Map<String, Object>> attendance(long studentId, LocalDate since) {
        return db.query("select status,count(*) total from attendance_record where student_id=? and attendance_date>=? group by status", RowMaps.mapper(), studentId, since);
    }

    public List<Map<String, Object>> behavior(long studentId) {
        return db.query("select category,score,occurred_on from behavior_record where student_id=? order by occurred_on desc limit 20", RowMaps.mapper(), studentId);
    }

    public List<Map<String, Object>> skills(long studentId) {
        return db.query("select skill_name,score,level,occurred_on from skill_record where student_id=? order by occurred_on desc limit 20", RowMaps.mapper(), studentId);
    }

    public List<Map<String, Object>> tasks(long studentId) {
        return db.query("select gt.module,gt.title,st.status,gt.due_on from student_task st join growth_task gt on gt.id=st.task_id where st.student_id=? order by gt.due_on desc limit 20", RowMaps.mapper(), studentId);
    }
}
