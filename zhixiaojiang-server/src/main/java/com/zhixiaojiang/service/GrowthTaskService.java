package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.GrowthModule;
import com.zhixiaojiang.common.util.RequestValues;
import com.zhixiaojiang.dao.TaskDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

/**
 * 六机成长任务：发布、指派、教师确认完成并发放机智币、任务评价。
 *
 * <p>任务按发布者隔离，指派与完成都要同时校验任务归属与学生归属；
 * 完成发币以 {@code task:记录号} 为幂等键，重复确认不会重复发币。
 */
@Service
public class GrowthTaskService {
    private final TaskDao tasks;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public GrowthTaskService(TaskDao tasks, TeacherScope scope, AuditRecorder audit) {
        this.tasks = tasks;
        this.scope = scope;
        this.audit = audit;
    }

    public Map<String, Object> list() {
        return Map.of("items", tasks.ofTeacher(scope.teacher()));
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        long id = tasks.insertTask(
                RequestValues.text(body, "module", GrowthModule.defaultLabel()),
                RequestValues.text(body, "title", "成长任务"),
                body.get("description") == null ? null : String.valueOf(body.get("description")),
                RequestValues.date(body.get("dueOn")),
                RequestValues.intValue(body.get("pointReward")),
                scope.teacher());
        audit.record("CREATE", "growth_task", id, "发布六机成长任务");
        return Map.of("id", id);
    }

    @Transactional
    public Map<String, Object> assign(long taskId, Object rawStudentIds) {
        scope.requireTask(taskId, false);
        if (rawStudentIds instanceof Collection<?> studentIds) for (Object raw : studentIds) {
            long studentId = Long.parseLong(String.valueOf(raw));
            scope.requireStudent(studentId);
            tasks.assign(taskId, studentId);
        }
        audit.record("ASSIGN", "growth_task", taskId, "分配成长任务");
        return Map.of("saved", true);
    }

    public Map<String, Object> studentsOfTask(long taskId) {
        long teacher = scope.teacher();
        scope.requireTask(taskId, false);
        return Map.of("items", tasks.studentsOfTask(taskId, teacher));
    }

    /** 教师确认完成：同一记录只发一次机智币。 */
    @Transactional
    public Map<String, Object> complete(long studentTaskId, String note) {
        scope.requireStudentTask(studentTaskId, true);
        int changed = tasks.complete(studentTaskId, note);
        if (changed == 1) {
            Map<String, Object> reward = tasks.rewardOf(studentTaskId);
            tasks.awardPoints(((Number) reward.get("studentId")).longValue(), reward.get("pointReward"),
                    "完成任务：" + reward.get("title"), "task:" + studentTaskId, scope.teacher());
            audit.record("COMPLETE", "student_task", studentTaskId, "确认任务完成并发放机智币");
        }
        return Map.of("saved", true, "awarded", changed == 1);
    }

    @Transactional
    public Map<String, Object> evaluate(long studentTaskId, String note) {
        scope.requireStudentTask(studentTaskId, false);
        tasks.evaluate(studentTaskId, note);
        audit.record("EVALUATE", "student_task", studentTaskId, "记录任务评价");
        return Map.of("saved", true);
    }
}
