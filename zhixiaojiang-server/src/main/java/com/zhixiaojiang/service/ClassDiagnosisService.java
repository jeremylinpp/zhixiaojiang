package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.util.RequestValues;
import com.zhixiaojiang.dao.DiagnosisMapper;
import com.zhixiaojiang.model.po.ClassDiagnosisRecord;
import com.zhixiaojiang.model.po.ClassTarget;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** 班级诊改：目标值、当前值、偏差、改进措施与复评记录。 */
@Service
public class ClassDiagnosisService {
    /** 八字循环：目标、标准、计划、实施、监测、诊断、改进、优化。 */
    private static final List<String> CYCLE = List.of("目标", "标准", "计划", "实施", "监测", "诊断", "改进", "优化");

    private final DiagnosisMapper diagnoses;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public ClassDiagnosisService(DiagnosisMapper diagnoses, TeacherScope scope, AuditRecorder audit) {
        this.diagnoses = diagnoses;
        this.scope = scope;
        this.audit = audit;
    }

    public Map<String, Object> list() {
        var classIds = scope.classIds();
        if (classIds.isEmpty()) return Map.of("items", List.of(), "cycle", CYCLE);
        return Map.of("items", diagnoses.targets(classIds), "cycle", CYCLE);
    }

    public Map<String, Object> records(long targetId) {
        scope.requireTarget(targetId);
        return Map.of("items", diagnoses.records(targetId));
    }

    @Transactional
    public Map<String, Object> addRecord(long targetId, Map<String, Object> body) {
        scope.requireTarget(targetId);
        ClassDiagnosisRecord record = new ClassDiagnosisRecord();
        record.setTargetId(targetId);
        record.setMeasure(RequestValues.text(body, "measure", "记录改进措施"));
        record.setReviewResult(body.get("reviewResult") == null ? null : String.valueOf(body.get("reviewResult")));
        record.setRecordedOn(RequestValues.date(body.get("recordedOn")));
        record.setCreatedBy(scope.teacher());
        diagnoses.insertRecord(record);
        long id = record.getId();
        audit.record("CREATE", "class_diagnosis_record", id, "记录班级诊改措施");
        return Map.of("id", id);
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        Object rawClass = body.get("classId");
        long classId = rawClass == null || String.valueOf(rawClass).isBlank() ? scope.defaultClass() : Long.parseLong(String.valueOf(rawClass));
        scope.requireClass(classId);
        ClassTarget target = new ClassTarget();
        target.setClassId(classId);
        target.setName(RequestValues.text(body, "name", "新诊改目标"));
        target.setTargetValue(RequestValues.decimal(body.get("targetValue")));
        target.setCurrentValue(RequestValues.decimal(body.getOrDefault("currentValue", 0)));
        target.setUnit(String.valueOf(body.getOrDefault("unit", "%")));
        target.setCreatedBy(scope.teacher());
        diagnoses.insertTarget(target);
        long id = target.getId();
        audit.record("CREATE", "class_target", id, "创建班级诊改目标");
        return Map.of("id", id);
    }

    @Transactional
    public Map<String, Object> update(long targetId, Map<String, Object> body) {
        scope.requireTarget(targetId);
        diagnoses.update(targetId,
                RequestValues.decimalOrNull(body.get("targetValue")),
                RequestValues.decimalOrNull(body.get("currentValue")),
                body.get("status") == null ? null : String.valueOf(body.get("status")));
        audit.record("UPDATE", "class_target", targetId, "更新班级诊改指标");
        return Map.of("saved", true);
    }

    @Transactional
    public Map<String, Object> review(long targetId, Map<String, Object> body) {
        scope.requireTarget(targetId);
        diagnoses.review(targetId,
                RequestValues.decimal(body.get("currentValue")),
                body.get("status") == null ? null : String.valueOf(body.get("status")));
        audit.record("REVIEW", "class_target", targetId, "记录班级诊改复评");
        return Map.of("saved", true);
    }
}
