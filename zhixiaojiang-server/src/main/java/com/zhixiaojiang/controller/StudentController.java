package com.zhixiaojiang.controller;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.StudentStatus;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RequestValues;
import com.zhixiaojiang.common.util.SqlParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 学生档案：列表、详情、成长相关的只读视图，以及新增、修改与归档。 */
@RestController
@RequestMapping("/api/v1")
public class StudentController {
    private final JdbcTemplate db;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public StudentController(JdbcTemplate db, TeacherScope scope, AuditRecorder audit) {
        this.db = db;
        this.scope = scope;
        this.audit = audit;
    }

    @GetMapping("/students")
    Map<String, Object> students(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest req) {
        int safePage = Math.max(1, page), safeSize = Math.min(100, Math.max(1, pageSize));
        var classIds = scope.classIds();
        if (classIds.isEmpty()) return ApiResult.ok(Map.of("items", List.of(), "page", safePage, "pageSize", safeSize, "total", 0));
        Object[] ids = classIds.toArray();
        String in = SqlParams.inClause(classIds);
        String like = "%" + q.trim() + "%";
        var items = db.queryForList("select s.id,s.student_no as studentNo,s.name,s.gender,s.status,coalesce(round(avg(g.score),1),0) growthIndex from student s left join growth_record g on g.student_id=s.id where s.class_id in " + in + " and s.status=? and (s.name like ? or s.student_no like ?) group by s.id order by s.id limit ? offset ?", SqlParams.append(ids, StudentStatus.ACTIVE.name(), like, like, safeSize, (safePage - 1) * safeSize));
        Integer total = db.queryForObject("select count(*) from student s where s.class_id in " + in + " and s.status=? and (s.name like ? or s.student_no like ?)", Integer.class, SqlParams.append(ids, StudentStatus.ACTIVE.name(), like, like));
        return ApiResult.ok(Map.of("items", items, "page", safePage, "pageSize", safeSize, "total", total == null ? 0 : total));
    }

    @GetMapping("/students/{id}")
    Map<String, Object> student(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id);
        var s = db.queryForMap("select id,student_no as studentNo,name,gender,status from student where id=?", id);
        return ApiResult.ok(Map.of("student", s, "growth", db.queryForList("select dimension,round(avg(score),1) score from growth_record where student_id=? group by dimension", id), "scores", db.queryForList("select subject,exam_name examName,score,full_score fullScore,occurred_on occurredOn from score_record where student_id=? order by occurred_on", id), "timeline", db.queryForList("select title,detail,occurred_on occurredOn,source from growth_record where student_id=? order by occurred_on desc", id)));
    }

    @GetMapping("/students/{id}/portrait")
    Map<String, Object> portrait(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id);
        return ApiResult.ok(Map.of("studentId", id, "dimensions", db.queryForList("select dimension,round(avg(score),1) score from growth_record where student_id=? group by dimension", id)));
    }

    @GetMapping("/students/{id}/timeline")
    Map<String, Object> timeline(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id);
        return ApiResult.ok(Map.of("items", db.queryForList("select title,detail,occurred_on occurredOn,source,created_by createdBy from growth_record where student_id=? order by occurred_on desc", id)));
    }

    @GetMapping("/students/{id}/attendance")
    Map<String, Object> attendance(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id);
        return ApiResult.ok(Map.of("items", db.queryForList("select id,attendance_date attendanceDate,status,note,created_by createdBy from attendance_record where student_id=? order by attendance_date desc", id)));
    }

    @GetMapping("/students/{id}/behavior")
    Map<String, Object> behavior(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id);
        return ApiResult.ok(Map.of("items", db.queryForList("select id,category,score,occurred_on occurredOn,detail,created_by createdBy from behavior_record where student_id=? order by occurred_on desc", id)));
    }

    @PostMapping("/students/{id}/behavior")
    Map<String, Object> recordBehavior(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requireStudent(id);
        long rid = JdbcInsert.returningId(db, "insert into behavior_record(student_id,category,score,occurred_on,detail,created_by) values(?,?,?,?,?,?)", id, RequestValues.text(b, "category", "日常表现"), RequestValues.decimalOrNull(b.get("score")), RequestValues.date(b.get("occurredOn")), b.get("detail"), scope.teacher());
        audit.record("CREATE", "behavior_record", rid, "记录学生行为表现");
        return ApiResult.ok(Map.of("id", rid));
    }

    @GetMapping("/students/{id}/skills")
    Map<String, Object> skills(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id);
        return ApiResult.ok(Map.of("items", db.queryForList("select id,skill_name skillName,score,level,occurred_on occurredOn,evidence,created_by createdBy from skill_record where student_id=? order by occurred_on desc", id)));
    }

    @GetMapping("/students/{id}/evaluations")
    Map<String, Object> evaluations(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id);
        return ApiResult.ok(Map.of("items", db.queryForList("select id,period_start periodStart,period_end periodEnd,moral_score moralScore,skill_score skillScore,thinking_score thinkingScore,smart_score smartScore,evidence,created_by createdBy from dimension_evaluation where student_id=? order by period_end desc", id)));
    }

    @PostMapping("/students")
    @Transactional
    Map<String, Object> createStudent(@Valid @RequestBody StudentInput b, HttpServletRequest req) {
        long classId = b.classId() == null ? scope.defaultClass() : b.classId();
        scope.requireClass(classId);
        long id = JdbcInsert.returningId(db, "insert into student(class_id,student_no,name,gender,status) values(?,?,?,?,?)", classId, b.studentNo().trim(), b.name().trim(), b.gender(), StudentStatus.ACTIVE.name());
        audit.record("CREATE", "student", id, "新增学生档案");
        return ApiResult.ok(Map.of("id", id));
    }

    @PutMapping("/students/{id}")
    @Transactional
    Map<String, Object> updateStudent(@PathVariable long id, @Valid @RequestBody StudentInput b, HttpServletRequest req) {
        scope.requireStudent(id);
        db.update("update student set name=?,gender=?,student_no=? where id=?", b.name().trim(), b.gender(), b.studentNo().trim(), id);
        audit.record("UPDATE", "student", id, "更新学生档案");
        return ApiResult.ok(Map.of("saved", true));
    }

    @PostMapping("/students/{id}/archive")
    @Transactional
    Map<String, Object> archiveStudent(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id);
        db.update("update student set status=?,archived_at=now() where id=?", StudentStatus.ARCHIVED.name(), id);
        audit.record("ARCHIVE", "student", id, "归档学生档案");
        return ApiResult.ok(Map.of("saved", true));
    }

    public record StudentInput(@NotBlank @jakarta.validation.constraints.Size(max = 80) String name,
                               @NotBlank @jakarta.validation.constraints.Size(max = 32) String studentNo,
                               @jakarta.validation.constraints.Size(max = 16) String gender,
                               Long classId) {
    }
}
