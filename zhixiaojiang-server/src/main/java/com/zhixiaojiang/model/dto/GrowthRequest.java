package com.zhixiaojiang.model.dto;

import com.zhixiaojiang.common.constant.GrowthDimension;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 成长记录录入请求。 */
public record GrowthRequest(@NotBlank @Pattern(regexp = GrowthDimension.PATTERN) String dimension,
                            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal score,
                            @NotBlank @Size(max = 160) String title,
                            @Size(max = 500) String detail,
                            @NotNull @PastOrPresent LocalDate occurredOn,
                            @NotBlank @Size(max = 64) String source) {
}
