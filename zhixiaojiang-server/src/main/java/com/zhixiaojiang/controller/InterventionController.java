package com.zhixiaojiang.controller;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.InterventionStatus;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.JsonValues;
import com.zhixiaojiang.common.util.RequestValues;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 一人一策：教师审核、执行过程记录、状态流转与阶段复评。 */
@RestController
@RequestMapping("/api/v1")
public class InterventionController {
    private final JdbcTemplate db;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public InterventionController(JdbcTemplate db, TeacherScope scope, AuditRecorder audit) {
        this.db = db;
        this.scope = scope;
        this.audit = audit;
    }

    @GetMapping("/interventions")
    Map<String, Object> interventions(HttpServletRequest req) {
        long teacher = scope.teacher(req);
        return ApiResult.ok(Map.of("items", db.queryForList("select i.id,i.title,i.status,i.teacher_note teacherNote,s.name studentName,i.review_at reviewAt from intervention_plan i join student s on s.id=i.student_id join class_room c on c.id=s.class_id where c.teacher_id=? order by i.updated_at desc limit 200", teacher)));
    }

    @PostMapping("/interventions")
    Map<String, Object> createIntervention(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        long sid = Long.parseLong(String.valueOf(b.get("studentId")));
        scope.requireStudent(sid, req);
        Long warningId = RequestValues.longOrNull(b.get("warningId"));
        if (warningId != null) requireWarningOfStudent(warningId, sid);
        String title = RequestValues.text(b, "title", "阶段成长支持方案");
        String suggestions = JsonValues.toJson(b.getOrDefault("suggestions", List.of("班主任个别谈话", "两周后复评")));
        long id = JdbcInsert.returningId(db, "insert into intervention_plan(student_id,warning_id,title,status,suggestions_json,teacher_note,review_at,created_by) values(?,?,?,?,?,?,?,?)", sid, warningId, title, InterventionStatus.DRAFT.name(), suggestions, b.get("teacherNote"), RequestValues.date(b.get("reviewAt")), scope.teacher(req));
        audit.record(req, "CREATE", "intervention_plan", id, warningId == null ? "创建帮扶草案" : "由预警 #" + warningId + " 创建帮扶草案");
        return ApiResult.ok(Map.of("id", id, "status", InterventionStatus.DRAFT.name()));
    }

    /**
     * 帮扶方案详情：方案本身 + 执行过程记录，供教师审核与复评页面使用。
     *
     * <p>这里显式构造 camelCase 键，不用 SQL 别名：H2（demo profile）会把未加引号的别名折叠成小写，
     * 而 MySQL 保留大小写，直接依赖别名会让两套数据库返回的 JSON 键不一致。
     */
    @GetMapping("/interventions/{id}")
    Map<String, Object> intervention(@PathVariable long id, HttpServletRequest req) {
        scope.requirePlan(id, req, false);
        Map<String, Object> plan = db.queryForObject(
                "select i.id,i.student_id,i.warning_id,i.title,i.status,i.suggestions_json,i.teacher_note,i.review_at,i.created_at,s.name from intervention_plan i join student s on s.id=i.student_id where i.id=?",
                (rs, n) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    Object warningId = rs.getObject("warning_id");
                    Object reviewAt = rs.getObject("review_at");
                    row.put("id", rs.getLong("id"));
                    row.put("studentId", rs.getLong("student_id"));
                    row.put("warningId", warningId == null ? null : ((Number) warningId).longValue());
                    row.put("studentName", rs.getString("name"));
                    row.put("title", rs.getString("title"));
                    row.put("status", rs.getString("status"));
                    row.put("suggestions", JsonValues.toList(rs.getString("suggestions_json")));
                    row.put("teacherNote", rs.getString("teacher_note"));
                    row.put("reviewAt", reviewAt == null ? null : reviewAt.toString());
                    row.put("createdAt", rs.getTimestamp("created_at").toString());
                    return row;
                }, id);
        var records = db.query("select id,action,status,occurred_on,result,created_by from intervention_record where plan_id=? order by id",
                (rs, n) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("action", rs.getString("action"));
                    row.put("status", rs.getString("status"));
                    row.put("occurredOn", rs.getObject("occurred_on").toString());
                    row.put("result", rs.getString("result"));
                    row.put("createdBy", rs.getLong("created_by"));
                    return row;
                }, id);
        return ApiResult.ok(Map.of("plan", plan, "records", records));
    }

    @PutMapping("/interventions/{id}")
    Map<String, Object> updateIntervention(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requirePlan(id, req, false);
        db.update("update intervention_plan set title=coalesce(?,title),teacher_note=coalesce(?,teacher_note),review_at=coalesce(?,review_at) where id=?", b.get("title"), b.get("teacherNote"), RequestValues.dateOrNull(b.get("reviewAt")), id);
        audit.record(req, "UPDATE", "intervention_plan", id, "更新帮扶草案");
        return ApiResult.ok(Map.of("saved", true));
    }

    @PostMapping("/interventions/{id}/transition")
    Map<String, Object> transitionIntervention(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        Map<String, Object> plan = scope.requirePlan(id, req, true);
        InterventionStatus current = InterventionStatus.of(String.valueOf(plan.get("status")));
        InterventionStatus target = InterventionStatus.of(RequestValues.text(b, "status", ""));
        if (current == null || target == null || !current.allowedNext().contains(target))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "非法帮扶状态转换");
        db.update("update intervention_plan set status=? where id=?", target.name(), id);
        audit.record(req, "TRANSITION", "intervention_plan", id, "帮扶状态变更为" + target.name());
        return ApiResult.ok(Map.of("saved", true, "status", target.name()));
    }

    @PostMapping("/interventions/{id}/review")
    Map<String, Object> reviewIntervention(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requirePlan(id, req, false);
        db.update("update intervention_plan set review_at=?,teacher_note=coalesce(?,teacher_note) where id=?", RequestValues.date(b.get("reviewAt")), b.get("teacherNote"), id);
        audit.record(req, "REVIEW", "intervention_plan", id, "记录阶段复评");
        return ApiResult.ok(Map.of("saved", true));
    }

    @PostMapping("/interventions/{id}/records")
    Map<String, Object> interventionRecord(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requirePlan(id, req, false);
        db.update("insert into intervention_record(plan_id,action,status,occurred_on,result,created_by) values(?,?,?,?,?,?)", id, b.get("action"), "DONE", LocalDate.now(), b.get("result"), scope.teacher(req));
        db.update("update intervention_plan set status=? where id=? and status=?", InterventionStatus.IN_PROGRESS.name(), id, InterventionStatus.CONFIRMED.name());
        audit.record(req, "CREATE", "intervention_record", id, "记录帮扶过程");
        return ApiResult.ok(Map.of("saved", true));
    }

    /** 预警必须属于同一个学生，避免把方案挂到其他班级的预警上。 */
    private void requireWarningOfStudent(long warningId, long studentId) {
        Integer owned = db.queryForObject("select count(*) from warning_record where id=? and student_id=?", Integer.class, warningId, studentId);
        if (owned == null || owned == 0)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "预警不存在或不属于该学生");
    }
}
