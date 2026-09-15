package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 机智币流水与积分规则的数据访问。积分账本只追加，撤销通过反向流水实现。 */
@Repository
public class PointDao {
    private final JdbcTemplate db;

    public PointDao(JdbcTemplate db) {
        this.db = db;
    }

    /** 流水按时间倒序；reversed 表示该笔是否已被撤销，reversalOf 指向被撤销的原流水。 */
    public List<Map<String, Object>> page(long studentId, int size, int offset) {
        return db.query("select p.id,p.amount,p.category,p.reason,p.created_at,p.idempotency_key,exists(select 1 from point_ledger r where r.idempotency_key=concat('reverse:',p.id)) reversed from point_ledger p where p.student_id=? order by p.id desc limit ? offset ?",
                (rs, n) -> {
                    Map<String, Object> row = RowMaps.of(rs);
                    String key = String.valueOf(row.remove("idempotencyKey"));
                    row.put("reversalOf", key.startsWith("reverse:") ? Long.parseLong(key.substring(8)) : null);
                    return row;
                }, studentId, size, offset);
    }

    public long count(long studentId) {
        Long total = db.queryForObject("select count(*) from point_ledger where student_id=?", Long.class, studentId);
        return total == null ? 0 : total;
    }

    public long balance(long studentId) {
        Long balance = db.queryForObject("select coalesce(sum(amount),0) from point_ledger where student_id=?", Long.class, studentId);
        return balance == null ? 0 : balance;
    }

    public Optional<Integer> enabledRuleAmount(long ruleId) {
        var amounts = db.queryForList("select amount from point_rule where id=? and enabled=true", Integer.class, ruleId);
        return amounts.stream().findFirst();
    }

    public int insert(long studentId, int amount, String category, String reason, String idempotencyKey, long actor) {
        return db.update("insert ignore into point_ledger(student_id,amount,category,reason,idempotency_key,created_by) values(?,?,?,?,?,?)",
                studentId, amount, category, reason, idempotencyKey, actor);
    }

    /** 幂等键查回已保存的流水，用于核对重复请求内容是否一致。 */
    public Map<String, Object> findByIdempotencyKey(String idempotencyKey) {
        return db.queryForObject("select id,student_id,amount,category,reason from point_ledger where idempotency_key=?", RowMaps.mapper(), idempotencyKey);
    }

    public Optional<Map<String, Object>> findById(long id) {
        return db.query("select id,student_id,amount,category,reason from point_ledger where id=?", RowMaps.mapper(), id).stream().findFirst();
    }

    public List<Map<String, Object>> enabledRules() {
        return db.query("select id,name,category,amount,enabled,description from point_rule where enabled=true order by id", RowMaps.mapper());
    }

    public long insertRule(String name, String category, int amount, Object enabled, String description, long actor) {
        return JdbcInsert.returningId(db, "insert into point_rule(name,category,amount,enabled,description,created_by) values(?,?,?,?,?,?)", name, category, amount, enabled, description, actor);
    }
}
