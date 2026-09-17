package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.PointCategory;
import com.zhixiaojiang.dao.PointMapper;
import com.zhixiaojiang.model.po.PointLedger;
import com.zhixiaojiang.model.po.PointRule;
import com.zhixiaojiang.model.vo.PointLedgerRow;
import com.zhixiaojiang.model.vo.PointRuleRow;
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
    private final PointMapper points;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public PointsService(PointMapper points, TeacherScope scope, AuditRecorder audit) {
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
        PointLedger ledger = new PointLedger();
        ledger.setStudentId(award.studentId());
        ledger.setAmount(amount);
        ledger.setCategory(category.name());
        ledger.setReason(reason);
        ledger.setIdempotencyKey(key);
        ledger.setCreatedBy(teacher);
        int changed = points.insert(ledger);
        PointLedgerRow saved = points.findByIdempotencyKey(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "积分入账失败，请重试"));
        if (saved.getStudentId() != award.studentId() || saved.getAmount() != amount
                || !reason.equals(saved.getReason()) || !category.name().equals(saved.getCategory()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "重复请求的内容发生变化，请重新创建记录");
        long id = saved.getId();
        if (changed == 1) audit.record(teacher, "CREATE", "point_ledger", id, "录入机智币");
        return Map.of("id", id, "saved", changed == 1, "idempotencyKey", award.idempotencyKey());
    }

    @Transactional
    public Map<String, Object> reverse(long ledgerId) {
        PointLedgerRow original = points.findById(ledgerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "积分记录不存在"));
        long teacher = scope.requireStudent(original.getStudentId());
        if (PointCategory.REVERSAL.name().equals(original.getCategory()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "反向流水不能再次撤销，请新建纠错记录");
        String reason = "撤销：" + original.getReason();
        PointLedger reversal = new PointLedger();
        reversal.setStudentId(original.getStudentId());
        reversal.setAmount(-original.getAmount());
        reversal.setCategory(PointCategory.REVERSAL.name());
        reversal.setReason(reason.substring(0, Math.min(160, reason.length())));
        reversal.setIdempotencyKey("reverse:" + ledgerId);
        reversal.setCreatedBy(teacher);
        int changed = points.insert(reversal);
        if (changed == 1) audit.record(teacher, "REVERSE", "point_ledger", ledgerId, "撤销机智币，保留原流水");
        return Map.of("saved", changed == 1);
    }

    /** 积分规则是全校共用的量纲目录，不含学生数据，因此按启用状态全局可读。 */
    public Map<String, Object> rules() {
        return Map.of("items", points.enabledRules());
    }

    @Transactional
    public Map<String, Object> createRule(Map<String, Object> body) {
        PointRule rule = new PointRule();
        rule.setName(com.zhixiaojiang.common.util.RequestValues.text(body, "name", "新积分规则"));
        rule.setCategory(com.zhixiaojiang.common.util.RequestValues.text(body, "category", PointCategory.MANUAL.name()));
        rule.setAmount(com.zhixiaojiang.common.util.RequestValues.intValue(body.get("amount")));
        rule.setEnabled(Boolean.parseBoolean(String.valueOf(body.getOrDefault("enabled", true))));
        rule.setDescription(body.get("description") == null ? null : String.valueOf(body.get("description")));
        rule.setCreatedBy(scope.teacher());
        points.insertRule(rule);
        long id = rule.getId();
        audit.record("CREATE", "point_rule", id, "创建积分规则");
        return Map.of("id", id);
    }
}
