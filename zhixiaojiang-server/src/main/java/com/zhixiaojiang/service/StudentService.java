package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.StudentStatus;
import com.zhixiaojiang.common.util.RequestValues;
import com.zhixiaojiang.dao.StudentMapper;
import com.zhixiaojiang.model.dto.StudentRequest;
import com.zhixiaojiang.model.po.BehaviorRecord;
import com.zhixiaojiang.model.po.Student;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** 学生档案：分页查询、详情与成长明细、新增、修改与归档。 */
@Service
public class StudentService {
    private final StudentMapper students;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public StudentService(StudentMapper students, TeacherScope scope, AuditRecorder audit) {
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
                "student", requireSummary(studentId),
                "growth", students.growthByDimension(studentId),
                "scores", students.scores(studentId),
                "timeline", students.timeline(studentId, false));
    }

    public Map<String, Object> portrait(long studentId) {
        scope.requireStudent(studentId);
        return Map.of("studentId", studentId, "dimensions", students.growthByDimension(studentId));
    }

    public Map<String, Object> timeline(long studentId) {
        scope.requireStudent(studentId);
        return Map.of("items", students.timeline(studentId, true));
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
        BehaviorRecord record = new BehaviorRecord();
        record.setStudentId(studentId);
        record.setCategory(RequestValues.text(body, "category", "日常表现"));
        record.setScore(RequestValues.decimalOrNull(body.get("score")));
        record.setOccurredOn(RequestValues.date(body.get("occurredOn")));
        record.setDetail(body.get("detail") == null ? null : String.valueOf(body.get("detail")));
        record.setCreatedBy(scope.teacher());
        students.insertBehavior(record);
        audit.record("CREATE", "behavior_record", record.getId(), "记录学生行为表现");
        return Map.of("id", record.getId());
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
        Student record = new Student();
        record.setClassId(classId);
        record.setStudentNo(request.studentNo().trim());
        record.setName(request.name().trim());
        record.setGender(request.gender());
        record.setStatus(StudentStatus.ACTIVE.name());
        students.insert(record);
        audit.record("CREATE", "student", record.getId(), "新增学生档案");
        return Map.of("id", record.getId());
    }

    @Transactional
    public Map<String, Object> update(long studentId, StudentRequest request) {
        scope.requireStudent(studentId);
        students.update(studentId, request.name().trim(), request.gender(), request.studentNo().trim());
        audit.record("UPDATE", "student", studentId, "更新学生档案");
        return Map.of("saved", true);
    }

    /** 归档学生：从在籍列表移除，历史成长、积分与帮扶记录全部保留。 */
    @Transactional
    public Map<String, Object> archive(long studentId) {
        scope.requireStudent(studentId);
        students.archive(studentId, StudentStatus.ARCHIVED.name());
        audit.record("ARCHIVE", "student", studentId, "归档学生档案");
        return Map.of("saved", true);
    }

    /** 归属校验已通过时档案必然存在；查不到说明数据不一致，按不可用处理。 */
    private com.zhixiaojiang.model.vo.StudentSummary requireSummary(long studentId) {
        var summary = students.findSummary(studentId);
        if (summary == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学生档案不存在或不属于当前教师");
        return summary;
    }
}
