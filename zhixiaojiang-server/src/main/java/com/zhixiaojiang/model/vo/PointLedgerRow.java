package com.zhixiaojiang.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * 机智币流水行。
 *
 * <p>幂等键只用于判断该笔是否已被撤销，不对外暴露：{@code reversalOf} 由它推导，
 * 与旧实现一致（reverse:&lt;原流水号&gt;）。
 */
public class PointLedgerRow {
    private Long id;
    private Long studentId;
    private Integer amount;
    private String category;
    private String reason;
    private LocalDateTime createdAt;
    private Boolean reversed;
    @JsonIgnore
    private String idempotencyKey;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getReversed() { return reversed; }
    public void setReversed(Boolean reversed) { this.reversed = reversed; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    /** 撤销流水指向被撤销的原流水号；普通流水为 null。 */
    public Long getReversalOf() {
        return idempotencyKey != null && idempotencyKey.startsWith("reverse:")
                ? Long.parseLong(idempotencyKey.substring(8))
                : null;
    }
}
