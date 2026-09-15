package com.zhixiaojiang.common;

import com.zhixiaojiang.auth.CurrentTeacher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 操作审计：涉及积分、预警、帮扶、任务、诊改和档案的写入都留下操作者、动作与摘要。
 */
@Component
public class AuditRecorder {
    private final JdbcTemplate db;
    private final CurrentTeacher current;

    public AuditRecorder(JdbcTemplate db, CurrentTeacher current) {
        this.db = db;
        this.current = current;
    }

    /** 由当前登录教师作为操作者记录。 */
    public void record(String action, String entityType, long entityId, String summary) {
        record(current.id(), action, entityType, entityId, summary);
    }

    /** 已解析出教师 id 时直接记录。 */
    public void record(long actor, String action, String entityType, long entityId, String summary) {
        db.update("insert into audit_log(actor_id,action,entity_type,entity_id,summary) values(?,?,?,?,?)", actor, action, entityType, entityId, summary);
    }
}
