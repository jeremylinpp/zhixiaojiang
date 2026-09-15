package com.zhixiaojiang.model.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 技能成长记录录入请求。 */
public record SkillRequest(@NotBlank @Size(max = 120) String skillName,
                           @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal score,
                           @Size(max = 32) String level,
                           @NotNull @PastOrPresent LocalDate occurredOn,
                           @NotBlank @Size(max = 500) String evidence) {
}
