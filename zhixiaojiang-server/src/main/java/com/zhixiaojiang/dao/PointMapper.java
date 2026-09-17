package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.PointLedger;
import com.zhixiaojiang.model.po.PointRule;
import com.zhixiaojiang.model.vo.PointLedgerRow;
import com.zhixiaojiang.model.vo.PointRuleRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/** 机智币流水与积分规则的数据访问。积分账本只追加，撤销通过反向流水实现。 */
@Mapper
public interface PointMapper {

    /** 流水按时间倒序；reversed 表示该笔是否已被撤销，reversalOf 由幂等键推导。 */
    List<PointLedgerRow> page(@Param("studentId") long studentId, @Param("size") int size, @Param("offset") int offset);

    long count(@Param("studentId") long studentId);

    long balance(@Param("studentId") long studentId);

    Optional<Integer> enabledRuleAmount(@Param("ruleId") long ruleId);

    /** 幂等键唯一，重复入账不会新增（insert ignore），返回新增行数。 */
    int insert(PointLedger ledger);

    /** 幂等键查回已保存的流水，用于核对重复请求内容是否一致。 */
    Optional<PointLedgerRow> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    Optional<PointLedgerRow> findById(@Param("id") long id);

    List<PointRuleRow> enabledRules();

    /** 返回自增主键并回填到 {@code rule.id}。 */
    int insertRule(PointRule rule);
}
