package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 成长工作台的数据访问：周期内的成绩、四维评价、成长记录、技能与出勤，以及五类录入。 */
@Repository
public class GrowthDao {
    private final JdbcTemplate db;

    public GrowthDao(JdbcTemplate db) {
        this.db = db;
    }

    public Map<String, Object> student(long studentId) {
        return db.queryForObject("select id,name,student_no,status from student where id=?", RowMaps.mapper(), studentId);
    }

    /** 周期内四维评价的平均分；缺失维度返回 null。 */
    public Map<String, Object> dimensionAverages(long studentId, LocalDate start, LocalDate end) {
        return db.queryForObject("select avg(moral_score) moral,avg(skill_score) skill,avg(thinking_score) thinking,avg(smart_score) smart from dimension_evaluation where student_id=? and period_end between ? and ?", RowMaps.mapper(), studentId, start, end);
    }

    public List<Map<String, Object>> scores(long studentId, LocalDate start, LocalDate end) {
        return db.query("select id,subject,exam_name,score,full_score,occurred_on,created_by from score_record where student_id=? and occurred_on between ? and ? order by occurred_on desc,id desc limit 100", RowMaps.mapper(), studentId, start, end);
    }

    public List<Map<String, Object>> evaluations(long studentId, LocalDate start, LocalDate end) {
        return db.query("select id,period_start,period_end,moral_score,skill_score,thinking_score,smart_score,evidence,created_by from dimension_evaluation where student_id=? and period_end between ? and ? order by period_end desc,id desc limit 100", RowMaps.mapper(), studentId, start, end);
    }

    public List<Map<String, Object>> growth(long studentId, LocalDate start, LocalDate end) {
        return db.query("select id,dimension,score,title,detail,occurred_on,source,created_by from growth_record where student_id=? and occurred_on between ? and ? order by occurred_on desc,id desc limit 100", RowMaps.mapper(), studentId, start, end);
    }

    public List<Map<String, Object>> attendance(long studentId, LocalDate start, LocalDate end) {
        return db.query("select id,attendance_date,status,note,created_by from attendance_record where student_id=? and attendance_date between ? and ? order by attendance_date desc limit 100", RowMaps.mapper(), studentId, start, end);
    }

    public List<Map<String, Object>> skills(long studentId, LocalDate start, LocalDate end) {
        return db.query("select id,skill_name,score,level,occurred_on,evidence,created_by from skill_record where student_id=? and occurred_on between ? and ? order by occurred_on desc,id desc limit 100", RowMaps.mapper(), studentId, start, end);
    }

    public int countSameExam(long studentId, String subject, String examName) {
        Integer count = db.queryForObject("select count(*) from score_record where student_id=? and subject=? and exam_name=?", Integer.class, studentId, subject, examName);
        return count == null ? 0 : count;
    }

    public long insertExam(long studentId, String subject, String examName, BigDecimal score, BigDecimal fullScore, LocalDate occurredOn, long actor) {
        return JdbcInsert.returningId(db, "insert into score_record(student_id,subject,exam_name,score,full_score,occurred_on,created_by) values(?,?,?,?,?,?,?)", studentId, subject, examName, score, fullScore, occurredOn, actor);
    }

    public List<Long> attendanceIdOn(long studentId, LocalDate date) {
        return db.queryForList("select id from attendance_record where student_id=? and attendance_date=?", Long.class, studentId, date);
    }

    public long insertAttendance(long studentId, LocalDate date, String status, String note, long actor) {
        return JdbcInsert.returningId(db, "insert into attendance_record(student_id,attendance_date,status,note,created_by) values(?,?,?,?,?)", studentId, date, status, note, actor);
    }

    public int updateAttendance(long id, String status, String note, long actor) {
        return db.update("update attendance_record set status=?,note=?,created_by=? where id=?", status, note, actor, id);
    }

    public long insertSkill(long studentId, String skillName, BigDecimal score, String level, LocalDate occurredOn, String evidence, long actor) {
        return JdbcInsert.returningId(db, "insert into skill_record(student_id,skill_name,score,level,occurred_on,evidence,created_by) values(?,?,?,?,?,?,?)", studentId, skillName, score, level, occurredOn, evidence, actor);
    }

    public long insertGrowth(long studentId, String dimension, BigDecimal score, String title, String detail, LocalDate occurredOn, String source, long actor) {
        return JdbcInsert.returningId(db, "insert into growth_record(student_id,dimension,score,title,detail,occurred_on,source,created_by) values(?,?,?,?,?,?,?,?)", studentId, dimension, score, title, detail, occurredOn, source, actor);
    }

    public long insertEvaluation(long studentId, LocalDate periodStart, LocalDate periodEnd, BigDecimal moral, BigDecimal skill, BigDecimal thinking, BigDecimal smart, String evidence, long actor) {
        return JdbcInsert.returningId(db, "insert into dimension_evaluation(student_id,period_start,period_end,moral_score,skill_score,thinking_score,smart_score,evidence,created_by) values(?,?,?,?,?,?,?,?,?)", studentId, periodStart, periodEnd, moral, skill, thinking, smart, evidence, actor);
    }
}
