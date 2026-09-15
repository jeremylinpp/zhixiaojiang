package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.util.RequestValues;
import com.zhixiaojiang.dao.StudentDao;
import com.zhixiaojiang.model.dto.StudentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** 学生档案：分页查询、详情与成长明细、新增、修改与归档。 */
@Service
public class StudentService {
    private final StudentDao students;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public StudentService(StudentDao students, TeacherScope scope, AuditRecorder audit) {
        this.students = students;
        this.scope = scope;
        this.audit = audit;
    }

    public Map<String, Object> page(String q, int page, int pageSize) {
        int safePage = Math.max(1, page), safeSize = Math.min(100, Math.max(1, pageSize));
        var classIds = scope.classIds();
        if (classIds.isEmpty()) return Map.of("items", List.of(), "page", safePage, "pageSize", safeSize, "total", 0);
        String like = "%" + q.trim() + "%";
        return Map.of(
                "items", students.page(classIds, like, safeSize, (safePage - 1) * safeSize),
                "page", safePage,
                "pageSize", safeSize,
                "total", students.countActive(classIds, like));
    }

    public Map<String, Object> detail(long studentId) {
        scope.requireStudent(studentId);
        return Map.of(
                "student", students.findById(studentId).orElseThrow(),
                "growth", students.growthByDimension(studentId),
                "scores", students.scores(studentId),
                "timeline", students.timeline(studentId));
    }

    public Map<String, Object> portrait(long studentId) {
        scope.requireStudent(studentId);
        return Map.of("studentId", studentId, "dimensions", students.growthByDimension(studentId));
    }

    public Map<String, Object> timeline(long studentId) {
        scope.requireStudent(studentId);
        return Map.of("items", students.timelineWithAuthor(studentId));
    }

    public Map<String, Object> attendance(long studentId) {
        scope.requireStudent(studentId);
        return Map.of("items", students.attendance(studentId));
    }

    public Map<String, Object> behavior(long studentId) {
        scope.requireStudent(studentId);
        return Map.of("items", students.behavior(studentId));
    }

    /** 行为记录为历史遗留入口，字段保持宽松以兼容既有调用方。 */
    @Transactional
    public Map<String, Object> recordBehavior(long studentId, Map<String, Object> body) {
        scope.requireStudent(studentId);
        long id = students.insertBehavior(studentId,
                RequestValues.text(body, "category", "日常表现"),
                RequestValues.decimalOrNull(body.get("score")),
                RequestValues.date(body.get("occurredOn")),
                body.get("detail") == null ? null : String.valueOf(body.get("detail")),
                scope.teacher());
        audit.record("CREATE", "behavior_record", id, "记录学生行为表现");
        return Map.of("id", id);
    }

    public Map<String, Object> skills(long studentId) {
        scope.requireStudent(studentId);
        return Map.of("items", students.skills(studentId));
    }

    public Map<String, Object> evaluations(long studentId) {
        scope.requireStudent(studentId);
        return Map.of("items", students.evaluations(studentId));
    }

    @Transactional
    public Map<String, Object> create(StudentRequest request) {
        long classId = request.classId() == null ? scope.defaultClass() : request.classId();
        scope.requireClass(classId);
        long id = students.insert(classId, request.studentNo().trim(), request.name().trim(), request.gender());
        audit.record("CREATE", "student", id, "新增学生档案");
        return Map.of("id", id);
    }

    @Transactional
    public Map<String, Object> update(long studentId, StudentRequest request) {
        scope.requireStudent(studentId);
        students.update(studentId, request.name().trim(), request.gender(), request.studentNo().trim());
        audit.record("UPDATE", "student", studentId, "更新学生档案");
        return Map.of("saved", true);
    }

    /**
     * 归档学生：从在籍列表移除，历史成长、积分与帮扶记录全部保留。
     */
    @Transactional
    public Map<String, Object> archive(long studentId) {
        scope.requireStudent(studentId);
        students.archive(studentId);
        audit.record("ARCHIVE", "student", studentId, "归档学生档案");
        return Map.of("saved", true);
    }

}
