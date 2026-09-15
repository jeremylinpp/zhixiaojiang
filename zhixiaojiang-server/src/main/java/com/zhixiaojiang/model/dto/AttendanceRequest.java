package com.zhixiaojiang.model.dto;

import com.zhixiaojiang.common.constant.AttendanceStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** 每日出勤登记请求；同一天重复登记视为更新。 */
public record AttendanceRequest(@NotNull @PastOrPresent LocalDate attendanceDate,
                                @NotBlank @Pattern(regexp = AttendanceStatus.PATTERN) String status,
                                @Size(max = 200) String note) {
}
