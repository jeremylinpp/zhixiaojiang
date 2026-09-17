package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.WarningLevel;
import com.zhixiaojiang.common.constant.WarningRule;
import com.zhixiaojiang.common.constant.WarningStatus;
import com.zhixiaojiang.common.util.JsonValues;
import com.zhixiaojiang.dao.WarningMapper;
import com.zhixiaojiang.model.dto.WarningReviewRequest;
import com.zhixiaojiang.model.po.WarningRecord;
import com.zhixiaojiang.model.vo.WarningRow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 智能预警：列表与详情、教师研判与关闭、规则筛查。
 *
 * <p>规则只描述可核实的事实（成绩趋势、迟到、任务与活动参与），不产出结论；结论由教师研判给出。
 * 筛查按教师负责的班级逐人执行，同一规则在 OPEN 状态只保留一条记录。
 */
@Service
public class WarningService {
    private final WarningMapper warnings;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public WarningService(WarningMapper warnings, TeacherScope scope, AuditRecorder audit) {
        this.warnings = warnings;
        this.scope = scope;
        this.audit = audit;
    }

    public Map<String, Object> page(String status, String q, int page, int pageSize) {
        boolean all = WarningMapper.STATUS_ALL.equals(status);
        WarningStatus requested = status == null || status.isBlank() ? WarningStatus.OPEN : WarningStatus.of(status);
        if (!all && requested == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未知预警状态");
        String filter = all ? WarningMapper.STATUS_ALL : requested.name();
        int current = Math.max(1, page), size = Math.max(1, Math.min(100, pageSize));
        String like = "%" + q.trim() + "%";
        long teacher = scope.teacher();
        return Map.of(
                "items", warnings.page(teacher, filter, like, size, (current - 1) * size),
                "total", warnings.count(teacher, filter, like),
                "page", current,
                "pageSize", size);
    }

    public Map<String, Object> detail(long warningId) {
        var warning = warnings.findOwned(warningId, scope.teacher(), false).orElseThrow(() -> notFound());
        return Map.of("warning", warning, "events", warnings.events(warningId));
    }

    @Transactional
    public Map<String, Object> triage(long warningId, WarningReviewRequest review) {
        var warning = owned(warningId, true);
        checkStatus(warning, review);
        if (!WarningStatus.OPEN.name().equals(warning.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该预警已研判，不能重复提交");
        String level = review.escalate() ? WarningLevel.MANUAL.name() : warning.getLevel();
        warnings.markReviewed(warningId, level, review.note().trim());
        audit.record("TRIAGE", "warning_record", warningId, "教师研判：" + review.note().trim());
        return Map.of("saved", true, "status", WarningStatus.REVIEWED.name());
    }

    @Transactional
    public Map<String, Object> close(long warningId, WarningReviewRequest review) {
        var warning = owned(warningId, true);
        checkStatus(warning, review);
        warnings.markClosed(warningId);
        audit.record("CLOSE", "warning_record", warningId, "关闭原因：" + review.note().trim());
        return Map.of("saved", true, "status", WarningStatus.CLOSED.name());
    }

    /** 规则筛查：返回本次新增的预警条数。 */
    @Transactional
    public Map<String, Object> analyzeRules() {
        long teacher = scope.teacher();
        LocalDate today = LocalDate.now(), since = today.minusDays(13), priorSince = today.minusDays(27);
        int created = 0;
        for (long studentId : warnings.activeStudentIdsOf(teacher)) {
            List<Double> ratios = warnings.recentMathRatios(studentId);
            boolean decline = ratios.size() >= 4 && ratios.get(3) > ratios.get(2) && ratios.get(2) > ratios.get(1) && ratios.get(1) > ratios.get(0);
            boolean low = ratios.size() >= 2 && ratios.get(0) < 0.6 && ratios.get(1) < 0.6;
            if (decline || low) {
                created += raise(studentId, decline ? WarningLevel.FOCUS : WarningLevel.ATTENTION,
                        decline ? WarningRule.SCORE_DECLINE : WarningRule.SCORE_LOW,
                        decline ? "同科目最近四次考试连续下降" : "最近两次考试低于及格线",
                        "数学成绩趋势由规则引擎计算");
            }
            int late = warnings.lateCountSince(studentId, since);
            if (late >= 3) {
                created += raise(studentId, WarningLevel.ATTENTION, WarningRule.LATE_14D,
                        "最近14天迟到至少3次", "迟到次数=" + late);
            }
            int overdue = warnings.overdueTaskCount(studentId, today);
            if (overdue >= 2) {
                created += raise(studentId, WarningLevel.ATTENTION, WarningRule.TASK_OVERDUE,
                        "至少2项到期任务未完成", "未完成到期任务=" + overdue);
            }
            int recentActivity = warnings.activityCountSince(studentId, since);
            int priorActivity = warnings.activityCountBetween(studentId, priorSince, since);
            if (priorActivity >= 2 && recentActivity * 2 <= priorActivity) {
                created += raise(studentId, WarningLevel.ATTENTION, WarningRule.ACTIVITY_DROP,
                        "最近14天活动参与较前14天下降至少一半", "前期=" + priorActivity, "近期=" + recentActivity);
            }
        }
        audit.record("ANALYZE", "warning_record", 0, "执行规则预警分析");
        return Map.of("source", "TEMPLATE", "message", "已按规则完成趋势筛查", "created", created, "disclaimer", "AI辅助建议，仅供教师参考");
    }

    /** 规则命中时写一条待研判预警；唯一键 + insert ignore 保证重复筛查不新增。 */
    private int raise(long studentId, WarningLevel level, WarningRule rule, String summary, String... evidence) {
        WarningRecord record = new WarningRecord();
        record.setStudentId(studentId);
        record.setLevel(level.name());
        record.setRuleCode(rule.name());
        record.setSummary(summary);
        record.setEvidenceJson(JsonValues.toJson(List.of(evidence)));
        record.setStatus(WarningStatus.OPEN.name());
        return warnings.insertIfAbsent(record);
    }

    private WarningRow owned(long warningId, boolean lock) {
        return warnings.findOwned(warningId, scope.teacher(), lock).orElseThrow(this::notFound);
    }

    private void checkStatus(WarningRow warning, WarningReviewRequest review) {
        if (!java.util.Objects.equals(warning.getStatus(), review.expectedStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "预警状态已变化，请重新读取后处理");
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "预警不存在或不属于当前教师");
    }
}
