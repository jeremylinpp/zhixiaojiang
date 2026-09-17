package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.GrowthModule;
import com.zhixiaojiang.common.util.RequestValues;
import com.zhixiaojiang.dao.StudentGrowthMapper;
import com.zhixiaojiang.dao.TaskMapper;
import com.zhixiaojiang.dao.TaskSubmissionMapper;
import com.zhixiaojiang.model.po.ActivityRecord;
import com.zhixiaojiang.model.po.GrowthTask;
import com.zhixiaojiang.model.vo.TaskReward;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    private final TaskMapper tasks;
    private final TeacherScope scope;
    private final AuditRecorder audit;
    private final TaskSubmissionMapper submissions;
    private final StudentGrowthMapper growth;
    private final StudentMessageService messages;

    public GrowthTaskService(TaskMapper tasks, TaskSubmissionMapper submissions, StudentGrowthMapper growth,
                             TeacherScope scope, AuditRecorder audit, StudentMessageService messages) {
        this.tasks = tasks;
        this.submissions = submissions;
        this.growth = growth;
        this.scope = scope;
        this.audit = audit;
        this.messages=messages;
    }

    public Map<String, Object> list() {
        return Map.of("items", tasks.ofTeacher(scope.teacher()));
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        GrowthTask task = new GrowthTask();
        task.setModule(RequestValues.text(body, "module", GrowthModule.defaultLabel()));
        task.setTitle(RequestValues.text(body, "title", "成长任务"));
        task.setDescription(body.get("description") == null ? null : String.valueOf(body.get("description")));
        task.setDueOn(RequestValues.date(body.get("dueOn")));
        task.setPointReward(RequestValues.intValue(body.get("pointReward")));
        task.setCreatedBy(scope.teacher());
        tasks.insert(task);
        long id = task.getId();
        audit.record("CREATE", "growth_task", id, "发布六机成长任务");
        return Map.of("id", id);
    }

    @Transactional
    public Map<String, Object> assign(long taskId, Object rawStudentIds) {
        scope.requireTask(taskId, false);
        if (rawStudentIds instanceof Collection<?> studentIds) for (Object raw : studentIds) {
            long studentId = Long.parseLong(String.valueOf(raw));
            scope.requireStudent(studentId);
            if(tasks.assign(taskId, studentId)>0)messages.send(studentId,"task-assigned:"+taskId,"收到新任务","教师为你指派了成长任务，请查看要求与截止日期。","任务");
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
    public Map<String, Object> complete(long studentTaskId, String note, Long submissionId) {
        var assignment=scope.requireStudentTask(studentTaskId, true);
        if (note!=null && note.length()>500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"评价不能超过 500 字");
        if ("RETURNED".equals(assignment.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"请等待学生补充提交");
        if ("SUBMITTED".equals(assignment.getStatus())) {
            Long current=submissions.latestId(studentTaskId).orElse(null);
            if (submissionId==null || !submissionId.equals(current)) throw new ResponseStatusException(HttpStatus.CONFLICT,"提交版本已变化，请查看最新成果后确认");
            submissions.completeSubmission(submissionId,note,scope.teacher());
        }
        int changed = tasks.complete(studentTaskId, note);
        if (changed == 1) {
            TaskReward reward = tasks.rewardOf(studentTaskId).orElseThrow();
            tasks.awardPoints(reward.getStudentId(), reward.getPointReward(),
                    "完成任务：" + reward.getTitle(), "task:" + studentTaskId, scope.teacher());
            ActivityRecord activity=new ActivityRecord();
            activity.setStudentId(reward.getStudentId());
            activity.setActivityDate(java.time.LocalDate.now());
            activity.setActivityType("六机任务");
            activity.setStatus("PARTICIPATED");
            activity.setDetail("完成任务："+reward.getTitle());
            activity.setCreatedBy(scope.teacher());
            growth.insertActivity(activity);
            audit.record("COMPLETE", "student_task", studentTaskId, "确认任务完成并发放机智币");
            messages.send(reward.getStudentId(),"task-complete:"+studentTaskId,"任务已确认完成","任务评价与积分结果已更新，请查看。","任务");
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
