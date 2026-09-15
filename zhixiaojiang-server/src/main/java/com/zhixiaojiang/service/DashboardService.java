package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.dao.DashboardDao;
import com.zhixiaojiang.dao.OwnershipDao;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 班级数字驾驶舱：只统计当前教师负责班级的出勤、指标与待研判预警。 */
@Service
public class DashboardService {
    private final DashboardDao dashboard;
    private final OwnershipDao ownership;
    private final TeacherScope scope;

    public DashboardService(DashboardDao dashboard, OwnershipDao ownership, TeacherScope scope) {
        this.dashboard = dashboard;
        this.ownership = ownership;
        this.scope = scope;
    }

    public Map<String, Object> classOverview() {
        var classIds = scope.classIds();
        if (classIds.isEmpty())
            return Map.of("class", Map.of("name", "未关联班级", "grade", "—"), "metrics", emptyMetrics(), "targets", List.of(), "warnings", List.of(), "demo", true);
        var first = ownership.classOf(classIds.get(0)).orElse(Map.of("name", "未关联班级", "grade", "—"));
        Map<String, Object> classInfo = classIds.size() == 1
                ? Map.of("name", first.get("name"), "grade", first.get("grade"))
                : Map.of("name", "全部任教班级（" + classIds.size() + " 个）", "grade", String.valueOf(first.get("grade")) + " 等");
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("studentCount", dashboard.countActiveStudents(classIds));
        LocalDate attendanceDate = dashboard.latestAttendanceDate(classIds);
        metrics.put("attendanceDate", attendanceDate);
        if (attendanceDate == null) {
            metrics.put("activeRate", null);
            metrics.put("attendanceCompleteness", 0);
        } else {
            Map<String, Object> summary = dashboard.attendanceSummary(classIds, attendanceDate);
            metrics.put("activeRate", summary.get("attendanceRate"));
            metrics.put("attendanceCompleteness", summary.get("registeredCount"));
        }
        return Map.of("class", classInfo, "metrics", metrics, "targets", dashboard.targets(classIds), "warnings", dashboard.openWarnings(classIds), "demo", true);
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
