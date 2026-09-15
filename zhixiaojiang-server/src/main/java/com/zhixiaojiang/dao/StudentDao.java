package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.constant.StudentStatus;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RowMaps;
import com.zhixiaojiang.common.util.SqlParams;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 学生档案与其成长明细的只读查询、档案写操作。 */
@Repository
public class StudentDao {
    private final JdbcTemplate db;

    public StudentDao(JdbcTemplate db) {
        this.db = db;
    }

    /** 在籍学生分页；成长指数取成长记录平均值。 */
    public List<Map<String, Object>> page(List<Long> classIds, String like, int size, int offset) {
        return db.query("select s.id,s.student_no,s.name,s.gender,s.status,coalesce(round(avg(g.score),1),0) growth_index from student s left join growth_record g on g.student_id=s.id where s.class_id in " + SqlParams.inClause(classIds) + " and s.status=? and (s.name like ? or s.student_no like ?) group by s.id order by s.id limit ? offset ?",
                RowMaps.mapper(), SqlParams.append(classIds.toArray(), StudentStatus.ACTIVE.name(), like, like, size, offset));
    }

    public int countActive(List<Long> classIds, String like) {
        Integer total = db.queryForObject("select count(*) from student s where s.class_id in " + SqlParams.inClause(classIds) + " and s.status=? and (s.name like ? or s.student_no like ?)",
                Integer.class, SqlParams.append(classIds.toArray(), StudentStatus.ACTIVE.name(), like, like));
        return total == null ? 0 : total;
    }

    public Optional<Map<String, Object>> findById(long studentId) {
        return db.query("select id,student_no,name,gender,status from student where id=?", RowMaps.mapper(), studentId).stream().findFirst();
    }

    /** 四维平均分（按成长记录）。 */
    public List<Map<String, Object>> growthByDimension(long studentId) {
        return db.query("select dimension,round(avg(score),1) score from growth_record where student_id=? group by dimension", RowMaps.mapper(), studentId);
    }

    public List<Map<String, Object>> scores(long studentId) {
        return db.query("select subject,exam_name,score,full_score,occurred_on from score_record where student_id=? order by occurred_on", RowMaps.mapper(), studentId);
    }

    public List<Map<String, Object>> timeline(long studentId) {
        return db.query("select title,detail,occurred_on,source from growth_record where student_id=? order by occurred_on desc", RowMaps.mapper(), studentId);
    }

    public List<Map<String, Object>> timelineWithAuthor(long studentId) {
        return db.query("select title,detail,occurred_on,source,created_by from growth_record where student_id=? order by occurred_on desc", RowMaps.mapper(), studentId);
    }

    public List<Map<String, Object>> attendance(long studentId) {
        return db.query("select id,attendance_date,status,note,created_by from attendance_record where student_id=? order by attendance_date desc", RowMaps.mapper(), studentId);
    }

    public List<Map<String, Object>> behavior(long studentId) {
        return db.query("select id,category,score,occurred_on,detail,created_by from behavior_record where student_id=? order by occurred_on desc", RowMaps.mapper(), studentId);
    }

    public List<Map<String, Object>> skills(long studentId) {
        return db.query("select id,skill_name,score,level,occurred_on,evidence,created_by from skill_record where student_id=? order by occurred_on desc", RowMaps.mapper(), studentId);
    }

    public List<Map<String, Object>> evaluations(long studentId) {
        return db.query("select id,period_start,period_end,moral_score,skill_score,thinking_score,smart_score,evidence,created_by from dimension_evaluation where student_id=? order by period_end desc", RowMaps.mapper(), studentId);
    }

    public long insertBehavior(long studentId, String category, java.math.BigDecimal score, java.time.LocalDate occurredOn, String detail, long actor) {
        return JdbcInsert.returningId(db, "insert into behavior_record(student_id,category,score,occurred_on,detail,created_by) values(?,?,?,?,?,?)", studentId, category, score, occurredOn, detail, actor);
    }

    public long insert(long classId, String studentNo, String name, String gender) {
        return JdbcInsert.returningId(db, "insert into student(class_id,student_no,name,gender,status) values(?,?,?,?,?)", classId, studentNo, name, gender, StudentStatus.ACTIVE.name());
    }

    public int update(long studentId, String name, String gender, String studentNo) {
        return db.update("update student set name=?,gender=?,student_no=? where id=?", name, gender, studentNo, studentId);
    }

    public int archive(long studentId) {
        return db.update("update student set status=?,archived_at=now() where id=?", StudentStatus.ARCHIVED.name(), studentId);
    }
}
