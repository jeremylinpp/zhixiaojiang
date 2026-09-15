package com.zhixiaojiang.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 班级授权边界的唯一实现。
 *
 * <p>班主任只能访问 {@code class_room.teacher_id} 指向自己的班级，以及这些班级下的学生、成长记录、
 * 预警、帮扶方案、六机任务和诊改指标。业务控制器禁止再硬编码 {@code class_id=1}，也禁止使用
 * 「取不到登录人时默认为 1 号教师」这类降级逻辑。
 *
 * <p>越权统一返回 404，避免通过状态码探测其他班级是否存在该记录。
 */
@Component
public class TeacherScope {
    private final JdbcTemplate db;

    public TeacherScope(JdbcTemplate db) { this.db = db; }

    /** 当前登录教师 id；未登录直接 401。 */
    public long teacher(HttpServletRequest request) {
        Object actor = request.getAttribute("userId");
        if (actor == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        return Long.parseLong(actor.toString());
    }

    /** 当前教师负责的班级 id，按 id 升序；第一个用作默认写入目标。 */
    public List<Long> classIds(HttpServletRequest request) { return classIds(teacher(request)); }

    public List<Long> classIds(long teacher) {
        return db.queryForList("select id from class_room where teacher_id=? order by id", Long.class, teacher);
    }

    /** 默认写入班级：当前教师负责的第一个班级。 */
    public long defaultClass(HttpServletRequest request) {
        var ids = classIds(request);
        if (ids.isEmpty())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号尚未关联班级，请联系管理员");
        return ids.get(0);
    }

    /** 校验班级归属，返回教师 id。 */
    public long requireClass(long classId, HttpServletRequest request) { return requireClass(classId, request, false); }

    public long requireClass(long classId, HttpServletRequest request, boolean lock) {
        long teacher = teacher(request);
        if (!classIds(teacher).contains(classId)) throw notFound("班级不存在或不属于当前教师");
        if (lock) db.queryForList("select id from class_room where id=? for update", Long.class, classId);
        return teacher;
    }

    /** 校验学生归属，返回教师 id。lock=true 时锁定学生行，供并发写入使用。 */
    public long requireStudent(long studentId, HttpServletRequest request) { return requireStudent(studentId, request, false); }

    public long requireStudent(long studentId, HttpServletRequest request, boolean lock) {
        long teacher = teacher(request);
        var found = db.queryForList("select s.id from student s join class_room c on c.id=s.class_id where s.id=? and c.teacher_id=?"
                + (lock ? " for update" : ""), Long.class, studentId, teacher);
        if (found.isEmpty()) throw notFound("学生档案不存在或不属于当前教师");
        return teacher;
    }

    /** 校验帮扶方案归属，返回该方案当前行（含 status）。 */
    public Map<String, Object> requirePlan(long planId, HttpServletRequest request, boolean lock) {
        long teacher = teacher(request);
        var plans = db.queryForList("select i.id,i.student_id,i.status from intervention_plan i join student s on s.id=i.student_id join class_room c on c.id=s.class_id where i.id=? and c.teacher_id=?"
                + (lock ? " for update" : ""), planId, teacher);
        if (plans.isEmpty()) throw notFound("帮扶方案不存在或不属于当前教师");
        return plans.get(0);
    }

    /** 校验班级诊改指标归属，返回所属班级 id。 */
    public long requireTarget(long targetId, HttpServletRequest request) {
        long teacher = teacher(request);
        var targets = db.queryForList("select t.class_id from class_target t join class_room c on c.id=t.class_id where t.id=? and c.teacher_id=?", Long.class, targetId, teacher);
        if (targets.isEmpty()) throw notFound("诊改指标不存在或不属于当前教师");
        return targets.get(0);
    }

    /** 校验六机任务由当前教师发布，返回任务行。 */
    public Map<String, Object> requireTask(long taskId, HttpServletRequest request, boolean lock) {
        long teacher = teacher(request);
        var tasks = db.queryForList("select id,module,title,point_reward,status from growth_task where id=? and created_by=?"
                + (lock ? " for update" : ""), taskId, teacher);
        if (tasks.isEmpty()) throw notFound("成长任务不存在或不属于当前教师");
        return tasks.get(0);
    }

    /** 校验学生任务：任务由当前教师发布，且学生属于当前教师的班级。 */
    public Map<String, Object> requireStudentTask(long studentTaskId, HttpServletRequest request, boolean lock) {
        long teacher = teacher(request);
        var rows = db.queryForList("select st.id,st.student_id,st.status,g.point_reward,g.title from student_task st join growth_task g on g.id=st.task_id join student s on s.id=st.student_id join class_room c on c.id=s.class_id where st.id=? and c.teacher_id=? and g.created_by=?"
                + (lock ? " for update" : ""), studentTaskId, teacher, teacher);
        if (rows.isEmpty()) throw notFound("学生任务不存在或不属于当前教师");
        return rows.get(0);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
