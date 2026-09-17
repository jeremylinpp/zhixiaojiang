package com.zhixiaojiang.common;

import com.zhixiaojiang.auth.CurrentTeacher;
import com.zhixiaojiang.dao.AuditMapper;
import com.zhixiaojiang.model.po.AuditLog;
import org.springframework.stereotype.Component;

/**
 * 操作审计：涉及积分、预警、帮扶、任务、诊改和档案的写入都留下操作者、动作与摘要。
 */
@Component
public class AuditRecorder {
    private final AuditMapper audit;
    private final CurrentTeacher current;

    public AuditRecorder(AuditMapper audit, CurrentTeacher current) {
        this.audit = audit;
        this.current = current;
    }

    /** 由当前登录教师作为操作者记录。 */
    public void record(String action, String entityType, long entityId, String summary) {
        record(current.id(), action, entityType, entityId, summary);
    }

    /** 已解析出教师 id 时直接记录。 */
    public void record(long actor, String action, String entityType, long entityId, String summary) {
        AuditLog record = new AuditLog();
        record.setActorId(actor);
        record.setAction(action);
        record.setEntityType(entityType);
        record.setEntityId(entityId);
        record.setSummary(summary);
        audit.insert(record);
    }
}
