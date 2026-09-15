package com.zhixiaojiang.controller;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.common.constant.StudentStatus;
import com.zhixiaojiang.common.constant.WarningStatus;
import com.zhixiaojiang.common.util.SqlParams;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 班级数字驾驶舱：只统计当前教师负责班级的出勤、指标与待研判预警。 */
@RestController
@RequestMapping("/api/v1")
public class DashboardController {
    private final JdbcTemplate db;
    private final TeacherScope scope;

    public DashboardController(JdbcTemplate db, TeacherScope scope) {
        this.db = db;
        this.scope = scope;
    }

    @GetMapping("/dashboard/class")
    Map<String, Object> dashboard(HttpServletRequest req) {
        var classIds = scope.classIds(req);
        if (classIds.isEmpty())
            return ApiResult.ok(Map.of("class", Map.of("name", "未关联班级", "grade", "—"), "metrics", emptyMetrics(), "targets", List.of(), "warnings", List.of(), "demo", true));
        var first = db.queryForMap("select name,grade from class_room where id=?", classIds.get(0));
        Map<String, Object> classInfo = classIds.size() == 1
                ? Map.of("name", first.get("name"), "grade", first.get("grade"))
                : Map.of("name", "全部任教班级（" + classIds.size() + " 个）", "grade", String.valueOf(first.get("grade")) + " 等");
        Object[] ids = classIds.toArray();
        String in = SqlParams.inClause(classIds);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("studentCount", Optional.ofNullable(db.queryForObject("select count(*) from student where class_id in " + in + " and status=?", Integer.class, SqlParams.append(ids, StudentStatus.ACTIVE.name()))).orElse(0));
        LocalDate attendanceDate = db.queryForObject("select max(ar.attendance_date) from attendance_record ar join student s on s.id=ar.student_id where s.class_id in " + in + " and s.status=?", LocalDate.class, SqlParams.append(ids, StudentStatus.ACTIVE.name()));
        metrics.put("attendanceDate", attendanceDate);
        if (attendanceDate == null) {
            metrics.put("activeRate", null);
            metrics.put("attendanceCompleteness", 0);
        } else {
            // 出勤率的分子为“出勤+迟到”，属于查询语义而非代码取值，集合字面量保留在 SQL 中。
            Map<String, Object> a = db.queryForMap("select count(*) registeredCount, round(100.0*sum(case when ar.status in ('PRESENT','LATE') then 1 else 0 end)/nullif(count(*),0),1) attendanceRate from attendance_record ar join student s on s.id=ar.student_id where s.class_id in " + in + " and s.status=? and ar.attendance_date=?", SqlParams.append(SqlParams.append(ids, StudentStatus.ACTIVE.name()), attendanceDate));
            metrics.put("activeRate", a.get("attendanceRate"));
            metrics.put("attendanceCompleteness", a.get("registeredCount"));
        }
        var targets = db.queryForList("select name,target_value as targetValue,current_value as currentValue,unit,status from class_target where class_id in " + in, ids);
        var warnings = db.queryForList("select w.id,w.level,w.summary,s.name studentName from warning_record w join student s on s.id=w.student_id where s.class_id in " + in + " and w.status=? order by w.created_at desc limit 50", SqlParams.append(ids, WarningStatus.OPEN.name()));
        return ApiResult.ok(Map.of("class", classInfo, "metrics", metrics, "targets", targets, "warnings", warnings, "demo", true));
    }

    private Map<String, Object> emptyMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("studentCount", 0);
        metrics.put("attendanceDate", null);
        metrics.put("activeRate", null);
        metrics.put("attendanceCompleteness", 0);
        return metrics;
    }
}
