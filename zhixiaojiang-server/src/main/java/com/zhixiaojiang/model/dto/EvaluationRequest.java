package com.zhixiaojiang.model.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 四维周期评价录入请求；允许部分维度缺失。 */
public record EvaluationRequest(@NotNull @PastOrPresent LocalDate periodStart,
                                @NotNull @PastOrPresent LocalDate periodEnd,
                                @DecimalMin("0") @DecimalMax("100") BigDecimal moralScore,
                                @DecimalMin("0") @DecimalMax("100") BigDecimal skillScore,
                                @DecimalMin("0") @DecimalMax("100") BigDecimal thinkingScore,
                                @DecimalMin("0") @DecimalMax("100") BigDecimal smartScore,
                                @NotBlank @Size(max = 1000) String evidence) {
}
