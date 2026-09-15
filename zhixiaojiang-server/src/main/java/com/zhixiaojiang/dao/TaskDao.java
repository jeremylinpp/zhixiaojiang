package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.constant.PointCategory;
import com.zhixiaojiang.common.constant.StudentTaskStatus;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 六机成长任务与指派记录的数据访问。
 *
 * <p>任务没有班级字段，按发布者隔离：{@code growth_task.created_by} 即归属。
 * 新任务状态固定为 PUBLISHED（取值域尚未定义，暂不建枚举）。
 */
@Repository
public class TaskDao {
    private final JdbcTemplate db;

    public TaskDao(JdbcTemplate db) {
        this.db = db;
    }

    public List<Map<String, Object>> ofTeacher(long teacherId) {
        return db.query("select id,module,title,description,due_on,point_reward,status from growth_task where created_by=? order by due_on", RowMaps.mapper(), teacherId);
    }

    public long insertTask(String module, String title, String description, LocalDate dueOn, int pointReward, long actor) {
        return JdbcInsert.returningId(db, "insert into growth_task(module,title,description,due_on,point_reward,status,created_by) values(?,?,?,?,?,'PUBLISHED',?)",
                module, title, description, dueOn, pointReward, actor);
    }

    public int assign(long taskId, long studentId) {
        return db.update("insert ignore into student_task(task_id,student_id,status) values(?,?,?)", taskId, studentId, StudentTaskStatus.ASSIGNED.name());
    }

    public List<Map<String, Object>> studentsOfTask(long taskId, long teacherId) {
        return db.query("select st.id,st.student_id,s.name student_name,st.status,st.completed_on,st.teacher_note from student_task st join student s on s.id=st.student_id join class_room c on c.id=s.class_id where st.task_id=? and c.teacher_id=?", RowMaps.mapper(), taskId, teacherId);
    }

    public int complete(long studentTaskId, String note) {
        return db.update("update student_task set status=?,completed_on=coalesce(completed_on,curdate()),teacher_note=? where id=? and status<>?",
                StudentTaskStatus.COMPLETED.name(), note, studentTaskId, StudentTaskStatus.COMPLETED.name());
    }

    /** 完成任务所需的学生与奖励信息。 */
    public Map<String, Object> rewardOf(long studentTaskId) {
        return db.queryForObject("select st.student_id,g.point_reward,g.title from student_task st join growth_task g on g.id=st.task_id where st.id=?", RowMaps.mapper(), studentTaskId);
    }

    /** 任务奖励发币：幂等键固定为 task:记录号，重复确认不会重复发币。 */
    public int awardPoints(long studentId, Object amount, String reason, String idempotencyKey, long actor) {
        return db.update("insert ignore into point_ledger(student_id,amount,category,reason,idempotency_key,created_by) values(?,?,?,?,?,?)",
                studentId, amount, PointCategory.TASK.name(), reason, idempotencyKey, actor);
    }

    public int evaluate(long studentTaskId, String note) {
        return db.update("update student_task set teacher_note=? where id=?", note, studentTaskId);
    }

    public Optional<Map<String, Object>> findTask(long taskId) {
        return db.query("select id,module,title,point_reward,status from growth_task where id=?", RowMaps.mapper(), taskId).stream().findFirst();
    }
}
