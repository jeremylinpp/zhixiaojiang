package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.InterventionStatus;
import com.zhixiaojiang.common.util.JsonValues;
import com.zhixiaojiang.common.util.RequestValues;
import com.zhixiaojiang.dao.InterventionMapper;
import com.zhixiaojiang.model.po.InterventionPlan;
import com.zhixiaojiang.model.po.InterventionRecord;
import com.zhixiaojiang.model.dto.InterventionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 一人一策：方案创建、审核、执行过程记录、状态流转与阶段复评。
 *
 * <p>AI 与规则只提供建议，方案默认是草稿，必须经教师确认后才能进入执行中；
 * 已结束的状态不再回流，需要继续帮扶时新建方案。
 */
@Service
public class InterventionService {
    private final InterventionMapper plans;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public InterventionService(InterventionMapper plans, TeacherScope scope, AuditRecorder audit) {
        this.plans = plans;
        this.scope = scope;
        this.audit = audit;
    }

    public Map<String, Object> list() {
        return Map.of("items", plans.ofTeacher(scope.teacher()));
    }

    public Map<String, Object> detail(long planId) {
        scope.requirePlan(planId, false);
        var plan = plans.detail(planId).orElseThrow(() -> notFound());
        return Map.of("plan", plan, "records", plans.records(planId));
    }

    @Transactional
    public Map<String, Object> create(InterventionRequest request) {
        scope.requireStudent(request.studentId());
        if (request.warningId() != null && !plans.warningBelongsToStudent(request.warningId(), request.studentId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "预警不存在或不属于该学生");
        String title = request.title() == null || request.title().isBlank() ? "阶段成长支持方案" : request.title().trim();
        List<String> suggestions = request.suggestions() == null || request.suggestions().isEmpty()
                ? List.of("班主任个别谈话", "两周后复评")
                : request.suggestions();
        InterventionPlan plan = new InterventionPlan();
        plan.setStudentId(request.studentId());
        plan.setWarningId(request.warningId());
        plan.setTitle(title);
        plan.setStatus(InterventionStatus.DRAFT.name());
        plan.setSuggestionsJson(JsonValues.toJson(suggestions));
        plan.setTeacherNote(request.teacherNote());
        plan.setReviewAt(request.reviewAt());
        plan.setCreatedBy(scope.teacher());
        plans.insert(plan);
        long id = plan.getId();
        audit.record("CREATE", "intervention_plan", id, request.warningId() == null ? "创建帮扶草案" : "由预警 #" + request.warningId() + " 创建帮扶草案");
        return Map.of("id", id, "status", InterventionStatus.DRAFT.name());
    }

    @Transactional
    public Map<String, Object> update(long planId, Map<String, Object> body) {
        scope.requirePlan(planId, false);
        plans.update(planId,
                body.get("title") == null ? null : String.valueOf(body.get("title")),
                body.get("teacherNote") == null ? null : String.valueOf(body.get("teacherNote")),
                RequestValues.dateOrNull(body.get("reviewAt")));
        audit.record("UPDATE", "intervention_plan", planId, "更新帮扶草案");
        return Map.of("saved", true);
    }

    @Transactional
    public Map<String, Object> transition(long planId, String nextStatus) {
        Map<String, Object> plan = scope.requirePlan(planId, true);
        InterventionStatus current = InterventionStatus.of(String.valueOf(plan.get("status")));
        InterventionStatus target = InterventionStatus.of(nextStatus == null ? "" : nextStatus);
        if (current == null || target == null || !current.allowedNext().contains(target))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "非法帮扶状态转换");
        plans.updateStatus(planId, target.name());
        audit.record("TRANSITION", "intervention_plan", planId, "帮扶状态变更为" + target.name());
        return Map.of("saved", true, "status", target.name());
    }

    @Transactional
    public Map<String, Object> review(long planId, Map<String, Object> body) {
        scope.requirePlan(planId, false);
        plans.updateReview(planId, RequestValues.date(body.get("reviewAt")),
                body.get("teacherNote") == null ? null : String.valueOf(body.get("teacherNote")));
        audit.record("REVIEW", "intervention_plan", planId, "记录阶段复评");
        return Map.of("saved", true);
    }

    @Transactional
    public Map<String, Object> addRecord(long planId, Map<String, Object> body) {
        scope.requirePlan(planId, false);
        InterventionRecord record = new InterventionRecord();
        record.setPlanId(planId);
        record.setAction(String.valueOf(body.get("action")));
        record.setStatus("DONE");
        record.setOccurredOn(java.time.LocalDate.now());
        record.setResult(body.get("result") == null ? null : String.valueOf(body.get("result")));
        record.setCreatedBy(scope.teacher());
        plans.insertRecord(record);
        plans.startIfConfirmed(planId, InterventionStatus.IN_PROGRESS.name(), InterventionStatus.CONFIRMED.name());
        audit.record("CREATE", "intervention_record", planId, "记录帮扶过程");
        return Map.of("saved", true);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "帮扶方案不存在或不属于当前教师");
    }
}
