package com.zhixiaojiang.model.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 考试成绩录入请求。 */
public record ExamRequest(@NotBlank @Size(max = 40) String subject,
                          @NotBlank @Size(max = 120) String examName,
                          @NotNull @DecimalMin("0") @DecimalMax("9999.99") BigDecimal score,
                          @NotNull @DecimalMin("0.01") @DecimalMax("9999.99") BigDecimal fullScore,
                          @NotNull @PastOrPresent LocalDate occurredOn) {
}
