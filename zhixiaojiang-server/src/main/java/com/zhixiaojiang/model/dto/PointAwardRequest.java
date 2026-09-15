package com.zhixiaojiang.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 机智币发放请求。
 *
 * <p>{@code idempotencyKey} 由调用方生成：同一键重复提交只入账一次，内容不一致则报冲突；
 * {@code ruleId} 存在时金额以服务端的规则额度为准，不接受前端传入的金额。
 */
public record PointAwardRequest(@Positive long studentId,
                                Integer amount,
                                Long ruleId,
                                @NotBlank @Size(max = 160) String reason,
                                @NotBlank @Size(max = 100) String idempotencyKey) {
}
