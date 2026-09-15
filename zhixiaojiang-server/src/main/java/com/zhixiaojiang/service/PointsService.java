package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.PointCategory;
import com.zhixiaojiang.dao.PointDao;
import com.zhixiaojiang.model.dto.PointAwardRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 机智币：余额与流水查询、按规则或手工发放、撤销纠错、积分规则维护。
 *
 * <p>账本只追加不修改：撤销时新增 REVERSAL 反向流水并保留原流水；
 * 幂等键由调用方提供，重复提交只入账一次，内容变化则报冲突；
 * 使用规则时金额以服务端规则额度为准，不接受前端传入的金额。
 */
@Service
public class PointsService {
    private final PointDao points;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public PointsService(PointDao points, TeacherScope scope, AuditRecorder audit) {
        this.points = points;
        this.scope = scope;
        this.audit = audit;
    }

    public Map<String, Object> ledger(long studentId, int page, int pageSize) {
        scope.requireStudent(studentId);
        int size = Math.max(1, Math.min(100, pageSize)), current = Math.max(1, page);
        return Map.of(
                "items", points.page(studentId, size, (current - 1) * size),
                "page", current,
                "pageSize", size,
                "total", points.count(studentId),
                "balance", points.balance(studentId));
    }

    @Transactional
    public Map<String, Object> award(PointAwardRequest award) {
        long teacher = scope.requireStudent(award.studentId());
        Integer amount = award.amount();
        PointCategory category = PointCategory.MANUAL;
        if (award.ruleId() != null) {
            amount = points.enabledRuleAmount(award.ruleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "积分规则不存在或已停用"));
            category = PointCategory.RULE;
        }
        if (amount == null || amount == 0 || amount < -1000 || amount > 1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "积分必须是 -1000 至 1000 的非零整数");
        String reason = award.reason().trim();
        String key = "teacher:" + teacher + ":" + award.idempotencyKey();
        int changed = points.insert(award.studentId(), amount, category.name(), reason, key, teacher);
        Map<String, Object> saved = points.findByIdempotencyKey(key);
        if (((Number) saved.get("studentId")).longValue() != award.studentId()
                || ((Number) saved.get("amount")).intValue() != amount
                || !reason.equals(saved.get("reason"))
                || !category.name().equals(saved.get("category")))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "重复请求的内容发生变化，请重新创建记录");
        long id = ((Number) saved.get("id")).longValue();
        if (changed == 1) audit.record(teacher, "CREATE", "point_ledger", id, "录入机智币");
        return Map.of("id", id, "saved", changed == 1, "idempotencyKey", award.idempotencyKey());
    }

    @Transactional
    public Map<String, Object> reverse(long ledgerId) {
        Map<String, Object> original = points.findById(ledgerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "积分记录不存在"));
        long teacher = scope.requireStudent(((Number) original.get("studentId")).longValue());
        if (PointCategory.REVERSAL.name().equals(original.get("category")))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "反向流水不能再次撤销，请新建纠错记录");
        String reason = "撤销：" + original.get("reason");
        int changed = points.insert(((Number) original.get("studentId")).longValue(),
                -((Number) original.get("amount")).intValue(), PointCategory.REVERSAL.name(),
                reason.substring(0, Math.min(160, reason.length())), "reverse:" + ledgerId, teacher);
        if (changed == 1) audit.record(teacher, "REVERSE", "point_ledger", ledgerId, "撤销机智币，保留原流水");
        return Map.of("saved", changed == 1);
    }

    /** 积分规则是全校共用的量纲目录，不含学生数据，因此按启用状态全局可读。 */
    public Map<String, Object> rules() {
        return Map.of("items", points.enabledRules());
    }

    @Transactional
    public Map<String, Object> createRule(Map<String, Object> body) {
        long id = points.insertRule(
                com.zhixiaojiang.common.util.RequestValues.text(body, "name", "新积分规则"),
                com.zhixiaojiang.common.util.RequestValues.text(body, "category", PointCategory.MANUAL.name()),
                com.zhixiaojiang.common.util.RequestValues.intValue(body.get("amount")),
                body.getOrDefault("enabled", true),
                body.get("description") == null ? null : String.valueOf(body.get("description")),
                scope.teacher());
        audit.record("CREATE", "point_rule", id, "创建积分规则");
        return Map.of("id", id);
    }
}
