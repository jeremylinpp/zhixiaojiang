package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 归属校验的数据访问：只回答“这条记录是否属于该教师的班级”，不含业务判断。
 *
 * <p>{@code lock=true} 时会锁定对应行，供状态流转等并发写入使用。
 * 授权边界（越权返回什么状态码）由 {@link com.zhixiaojiang.auth.TeacherScope} 决定。
 */
@Repository
public class OwnershipDao {
    private final JdbcTemplate db;

    public OwnershipDao(JdbcTemplate db) {
        this.db = db;
    }

    public List<Long> classIdsOf(long teacherId) {
        return db.queryForList("select id from class_room where teacher_id=? order by id", Long.class, teacherId);
    }

    public boolean classOwnedBy(long classId, long teacherId, boolean lock) {
        return !db.queryForList("select id from class_room where id=? and teacher_id=?" + (lock ? " for update" : ""), Long.class, classId, teacherId).isEmpty();
    }

    public boolean studentOwnedBy(long studentId, long teacherId, boolean lock) {
        return !db.queryForList("select s.id from student s join class_room c on c.id=s.class_id where s.id=? and c.teacher_id=?" + (lock ? " for update" : ""), Long.class, studentId, teacherId).isEmpty();
    }

    public Optional<Map<String, Object>> planOwnedBy(long planId, long teacherId, boolean lock) {
        var rows = db.query("select i.id,i.student_id,i.status from intervention_plan i join student s on s.id=i.student_id join class_room c on c.id=s.class_id where i.id=? and c.teacher_id=?" + (lock ? " for update" : ""), RowMaps.mapper(), planId, teacherId);
        return rows.stream().findFirst();
    }

    public Optional<Long> targetClassOwnedBy(long targetId, long teacherId) {
        var classes = db.queryForList("select t.class_id from class_target t join class_room c on c.id=t.class_id where t.id=? and c.teacher_id=?", Long.class, targetId, teacherId);
        return classes.stream().findFirst();
    }

    public Optional<Map<String, Object>> taskOwnedBy(long taskId, long teacherId, boolean lock) {
        var rows = db.query("select id,module,title,point_reward,status from growth_task where id=? and created_by=?" + (lock ? " for update" : ""), RowMaps.mapper(), taskId, teacherId);
        return rows.stream().findFirst();
    }

    public Optional<Map<String, Object>> studentTaskOwnedBy(long studentTaskId, long teacherId, boolean lock) {
        var rows = db.query("select st.id,st.student_id,st.status,g.point_reward,g.title from student_task st join growth_task g on g.id=st.task_id join student s on s.id=st.student_id join class_room c on c.id=s.class_id where st.id=? and c.teacher_id=? and g.created_by=?" + (lock ? " for update" : ""), RowMaps.mapper(), studentTaskId, teacherId, teacherId);
        return rows.stream().findFirst();
    }

    public Optional<Map<String, Object>> classOf(long classId) {
        var rows = db.query("select id,name,grade from class_room where id=?", RowMaps.mapper(), classId);
        return rows.stream().findFirst();
    }
}
