package com.zhixiaojiang.controller;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RequestValues;
import com.zhixiaojiang.common.util.SqlParams;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 班级诊改：目标值/当前值/偏差、改进措施与复评记录。 */
@RestController
@RequestMapping("/api/v1")
public class ClassDiagnosisController {
    private final JdbcTemplate db;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public ClassDiagnosisController(JdbcTemplate db, TeacherScope scope, AuditRecorder audit) {
        this.db = db;
        this.scope = scope;
        this.audit = audit;
    }

    @GetMapping("/class-diagnoses")
    Map<String, Object> diagnoses(HttpServletRequest req) {
        var cycle = List.of("目标", "标准", "计划", "实施", "监测", "诊断", "改进", "优化");
        var classIds = scope.classIds();
        if (classIds.isEmpty()) return ApiResult.ok(Map.of("items", List.of(), "cycle", cycle));
        return ApiResult.ok(Map.of("items", db.queryForList("select id,name,target_value targetValue,current_value currentValue,unit,status,round(target_value-current_value,1) deviation from class_target where class_id in " + SqlParams.inClause(classIds) + " order by id", classIds.toArray()), "cycle", cycle));
    }

    @GetMapping("/class-diagnoses/{id}/records")
    Map<String, Object> diagnosisRecords(@PathVariable long id, HttpServletRequest req) {
        scope.requireTarget(id);
        return ApiResult.ok(Map.of("items", db.queryForList("select id,target_id targetId,measure,review_result reviewResult,recorded_on recordedOn,created_by createdBy from class_diagnosis_record where target_id=? order by recorded_on desc", id)));
    }

    @PostMapping("/class-diagnoses/{id}/records")
    Map<String, Object> createDiagnosisRecord(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requireTarget(id);
        long rid = JdbcInsert.returningId(db, "insert into class_diagnosis_record(target_id,measure,review_result,recorded_on,created_by) values(?,?,?,?,?)", id, RequestValues.text(b, "measure", "记录改进措施"), b.get("reviewResult"), RequestValues.date(b.get("recordedOn")), scope.teacher());
        audit.record("CREATE", "class_diagnosis_record", rid, "记录班级诊改措施");
        return ApiResult.ok(Map.of("id", rid));
    }

    @PostMapping("/class-diagnoses")
    Map<String, Object> createDiagnosis(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        Object rawClass = b.get("classId");
        long classId = rawClass == null || String.valueOf(rawClass).isBlank() ? scope.defaultClass() : Long.parseLong(String.valueOf(rawClass));
        scope.requireClass(classId);
        long id = JdbcInsert.returningId(db, "insert into class_target(class_id,name,target_value,current_value,unit,status,created_by) values(?,?,?,?,?,'IN_PROGRESS',?)", classId, RequestValues.text(b, "name", "新诊改目标"), RequestValues.decimal(b.get("targetValue")), RequestValues.decimal(b.getOrDefault("currentValue", 0)), b.getOrDefault("unit", "%"), scope.teacher());
        audit.record("CREATE", "class_target", id, "创建班级诊改目标");
        return ApiResult.ok(Map.of("id", id));
    }

    @PutMapping("/class-diagnoses/{id}")
    Map<String, Object> updateDiagnosis(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requireTarget(id);
        db.update("update class_target set target_value=coalesce(?,target_value),current_value=coalesce(?,current_value),status=coalesce(?,status) where id=?", RequestValues.decimalOrNull(b.get("targetValue")), RequestValues.decimalOrNull(b.get("currentValue")), b.get("status"), id);
        audit.record("UPDATE", "class_target", id, "更新班级诊改指标");
        return ApiResult.ok(Map.of("saved", true));
    }

    @PostMapping("/class-diagnoses/{id}/review")
    Map<String, Object> reviewDiagnosis(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requireTarget(id);
        db.update("update class_target set current_value=?,status=coalesce(?,status) where id=?", RequestValues.decimal(b.get("currentValue")), b.get("status"), id);
        audit.record("REVIEW", "class_target", id, "记录班级诊改复评");
        return ApiResult.ok(Map.of("saved", true));
    }

}
