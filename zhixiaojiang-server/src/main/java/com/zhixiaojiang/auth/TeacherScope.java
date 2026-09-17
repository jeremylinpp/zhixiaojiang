package com.zhixiaojiang.auth;

import com.zhixiaojiang.dao.OwnershipMapper;
import com.zhixiaojiang.model.vo.PlanOwnership;
import com.zhixiaojiang.model.vo.StudentTaskOwnership;
import com.zhixiaojiang.model.vo.TaskRow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 班级授权边界的唯一实现。
 *
 * <p>班主任只能访问 {@code class_room.teacher_id} 指向自己的班级，以及这些班级下的学生、成长记录、
 * 预警、帮扶方案、六机任务和诊改指标。业务层与控制器都禁止硬编码 {@code class_id}，也禁止使用
 * 「取不到登录人时默认为 1 号教师」这类降级逻辑。
 *
 * <p>数据访问交给 {@link OwnershipMapper}，当前教师来自 {@link CurrentTeacher}，本类只负责把校验结果翻译成 HTTP 语义：未登录 401、
 * 越权统一 404（避免通过状态码探测其他班级是否存在该记录）。
 */
@Component
public class TeacherScope {
    private final OwnershipMapper ownership;
    private final CurrentTeacher current;

    public TeacherScope(OwnershipMapper ownership, CurrentTeacher current) {
        this.ownership = ownership;
        this.current = current;
    }

    /** 当前登录教师 id；未登录直接 401。 */
    public long teacher() {
        return current.id();
    }

    /** 当前教师负责的班级 id，按 id 升序；第一个用作默认写入目标。 */
    public List<Long> classIds() {
        return ownership.classIdsOf(teacher());
    }

    /** 默认写入班级：当前教师负责的第一个班级。 */
    public long defaultClass() {
        var ids = classIds();
        if (ids.isEmpty())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号尚未关联班级，请联系管理员");
        return ids.get(0);
    }

    /** 校验班级归属，返回教师 id。 */
    public long requireClass(long classId) {
        return requireClass(classId, false);
    }

    public long requireClass(long classId, boolean lock) {
        long teacher = teacher();
        if (ownership.classOwnedBy(classId, teacher, lock).isEmpty()) throw notFound("班级不存在或不属于当前教师");
        return teacher;
    }

    /** 校验学生归属，返回教师 id。lock=true 时锁定学生行，供并发写入使用。 */
    public long requireStudent(long studentId) {
        return requireStudent(studentId, false);
    }

    public long requireStudent(long studentId, boolean lock) {
        long teacher = teacher();
        if (ownership.studentOwnedBy(studentId, teacher, lock).isEmpty()) throw notFound("学生档案不存在或不属于当前教师");
        return teacher;
    }

    /** 校验帮扶方案归属，返回该方案当前状态与学生（供状态流转判断）。 */
    public PlanOwnership requirePlan(long planId, boolean lock) {
        return ownership.planOwnedBy(planId, teacher(), lock)
                .orElseThrow(() -> notFound("帮扶方案不存在或不属于当前教师"));
    }

    /** 校验班级诊改指标归属，返回所属班级 id。 */
    public long requireTarget(long targetId) {
        return ownership.targetClassOwnedBy(targetId, teacher())
                .orElseThrow(() -> notFound("诊改指标不存在或不属于当前教师"));
    }

    /** 校验六机任务由当前教师发布，返回任务行。 */
    public TaskRow requireTask(long taskId, boolean lock) {
        return ownership.taskOwnedBy(taskId, teacher(), lock)
                .orElseThrow(() -> notFound("成长任务不存在或不属于当前教师"));
    }

    /** 校验学生任务：任务由当前教师发布，且学生属于当前教师的班级。 */
    public StudentTaskOwnership requireStudentTask(long studentTaskId, boolean lock) {
        return ownership.studentTaskOwnedBy(studentTaskId, teacher(), lock)
                .orElseThrow(() -> notFound("学生任务不存在或不属于当前教师"));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
