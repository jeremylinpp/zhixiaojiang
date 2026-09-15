package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.JsonValues;
import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 一人一策方案与执行过程记录的数据访问。 */
@Repository
public class InterventionDao {
    private final JdbcTemplate db;

    public InterventionDao(JdbcTemplate db) {
        this.db = db;
    }

    public List<Map<String, Object>> ofTeacher(long teacherId) {
        return db.query("select i.id,i.title,i.status,i.teacher_note,s.name student_name,i.review_at from intervention_plan i join student s on s.id=i.student_id join class_room c on c.id=s.class_id where c.teacher_id=? order by i.updated_at desc limit 200", RowMaps.mapper(), teacherId);
    }

    /** 方案详情；建议列表由 JSON 列解析。 */
    public Optional<Map<String, Object>> detail(long planId) {
        var rows = db.query("select i.id,i.student_id,i.warning_id,i.title,i.status,i.suggestions_json,i.teacher_note,i.review_at,i.created_at,s.name student_name from intervention_plan i join student s on s.id=i.student_id where i.id=?", RowMaps.mapper(), planId);
        return rows.stream().findFirst().map(row -> {
            row.put("suggestions", JsonValues.toList(String.valueOf(row.remove("suggestionsJson"))));
            return row;
        });
    }

    public List<Map<String, Object>> records(long planId) {
        return db.query("select id,action,status,occurred_on,result,created_by from intervention_record where plan_id=? order by id", RowMaps.mapper(), planId);
    }

    public boolean warningBelongsToStudent(long warningId, long studentId) {
        Integer count = db.queryForObject("select count(*) from warning_record where id=? and student_id=?", Integer.class, warningId, studentId);
        return count != null && count > 0;
    }

    public long insert(long studentId, Long warningId, String title, String suggestionsJson, String teacherNote, LocalDate reviewAt, long actor) {
        return JdbcInsert.returningId(db, "insert into intervention_plan(student_id,warning_id,title,status,suggestions_json,teacher_note,review_at,created_by) values(?,?,?,?,?,?,?,?)",
                studentId, warningId, title, com.zhixiaojiang.common.constant.InterventionStatus.DRAFT.name(), suggestionsJson, teacherNote, reviewAt, actor);
    }

    public int update(long planId, String title, String teacherNote, LocalDate reviewAt) {
        return db.update("update intervention_plan set title=coalesce(?,title),teacher_note=coalesce(?,teacher_note),review_at=coalesce(?,review_at) where id=?", title, teacherNote, reviewAt, planId);
    }

    public int updateStatus(long planId, String status) {
        return db.update("update intervention_plan set status=? where id=?", status, planId);
    }

    public int updateReview(long planId, LocalDate reviewAt, String teacherNote) {
        return db.update("update intervention_plan set review_at=?,teacher_note=coalesce(?,teacher_note) where id=?", reviewAt, teacherNote, planId);
    }

    public int insertRecord(long planId, String action, String result, long actor) {
        return db.update("insert into intervention_record(plan_id,action,status,occurred_on,result,created_by) values(?,?,?,?,?,?)", planId, action, "DONE", LocalDate.now(), result, actor);
    }

    /** 已确认的方案一旦记录执行过程即进入执行中。 */
    public int startIfConfirmed(long planId) {
        return db.update("update intervention_plan set status=? where id=? and status=?", com.zhixiaojiang.common.constant.InterventionStatus.IN_PROGRESS.name(), planId, com.zhixiaojiang.common.constant.InterventionStatus.CONFIRMED.name());
    }
}
