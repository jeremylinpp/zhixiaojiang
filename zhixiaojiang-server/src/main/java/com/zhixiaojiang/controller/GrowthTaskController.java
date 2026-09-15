package com.zhixiaojiang.controller;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.GrowthModule;
import com.zhixiaojiang.common.constant.PointCategory;
import com.zhixiaojiang.common.constant.StudentTaskStatus;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RequestValues;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/** 六机成长任务：发布、指派、教师确认完成并发放机智币、任务评价。 */
@RestController
@RequestMapping("/api/v1")
public class GrowthTaskController {
    private final JdbcTemplate db;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public GrowthTaskController(JdbcTemplate db, TeacherScope scope, AuditRecorder audit) {
        this.db = db;
        this.scope = scope;
        this.audit = audit;
    }

    /** 六机任务是教师自建的班级任务，按发布者隔离；其他教师的班级不含这些任务。 */
    @GetMapping("/growth-tasks")
    Map<String, Object> tasks(HttpServletRequest req) {
        return ApiResult.ok(Map.of("items", db.queryForList("select id,module,title,description,due_on dueOn,point_reward pointReward,status from growth_task where created_by=? order by due_on", scope.teacher(req))));
    }

    @PostMapping("/growth-tasks")
    Map<String, Object> createTask(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        long id = JdbcInsert.returningId(db, "insert into growth_task(module,title,description,due_on,point_reward,status,created_by) values(?,?,?,?,?,'PUBLISHED',?)", RequestValues.text(b, "module", GrowthModule.defaultLabel()), RequestValues.text(b, "title", "成长任务"), b.get("description"), RequestValues.date(b.get("dueOn")), RequestValues.intValue(b.get("pointReward")), scope.teacher(req));
        audit.record(req, "CREATE", "growth_task", id, "发布六机成长任务");
        return ApiResult.ok(Map.of("id", id));
    }

    @PostMapping("/growth-tasks/{id}/assign")
    Map<String, Object> assignTask(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requireTask(id, req, false);
        Object raw = b.get("studentIds");
        if (raw instanceof Collection<?> ids) for (Object sid : ids) {
            long studentId = Long.parseLong(String.valueOf(sid));
            scope.requireStudent(studentId, req);
            db.update("insert ignore into student_task(task_id,student_id,status) values(?,?,?)", id, studentId, StudentTaskStatus.ASSIGNED.name());
        }
        audit.record(req, "ASSIGN", "growth_task", id, "分配成长任务");
        return ApiResult.ok(Map.of("saved", true));
    }

    @GetMapping("/growth-tasks/{id}/students")
    Map<String, Object> taskStudents(@PathVariable long id, HttpServletRequest req) {
        long teacher = scope.teacher(req);
        scope.requireTask(id, req, false);
        return ApiResult.ok(Map.of("items", db.queryForList("select st.id,st.student_id studentId,s.name studentName,st.status,st.completed_on completedOn,st.teacher_note teacherNote from student_task st join student s on s.id=st.student_id join class_room c on c.id=s.class_id where st.task_id=? and c.teacher_id=?", id, teacher)));
    }

    @PostMapping("/student-tasks/{id}/complete")
    @Transactional
    Map<String, Object> completeTask(@PathVariable long id, @RequestBody(required = false) Map<String, Object> b, HttpServletRequest req) {
        scope.requireStudentTask(id, req, true);
        int changed = db.update("update student_task set status=?,completed_on=coalesce(completed_on,curdate()),teacher_note=? where id=? and status<>?", StudentTaskStatus.COMPLETED.name(), b == null ? null : b.get("note"), id, StudentTaskStatus.COMPLETED.name());
        if (changed == 1) {
            Map<String, Object> t = db.queryForMap("select st.student_id,g.point_reward,g.title from student_task st join growth_task g on g.id=st.task_id where st.id=?", id);
            String key = "task:" + id;
            db.update("insert ignore into point_ledger(student_id,amount,category,reason,idempotency_key,created_by) values(?,?,?,?,?,?)", t.get("student_id"), t.get("point_reward"), PointCategory.TASK.name(), "完成任务：" + t.get("title"), key, scope.teacher(req));
            audit.record(req, "COMPLETE", "student_task", id, "确认任务完成并发放机智币");
        }
        return ApiResult.ok(Map.of("saved", true, "awarded", changed == 1));
    }

    @PostMapping("/student-tasks/{id}/evaluate")
    Map<String, Object> evaluateTask(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requireStudentTask(id, req, false);
        db.update("update student_task set teacher_note=? where id=?", b.get("note"), id);
        audit.record(req, "EVALUATE", "student_task", id, "记录任务评价");
        return ApiResult.ok(Map.of("saved", true));
    }
}
