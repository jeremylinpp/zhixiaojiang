package com.zhixiaojiang.model.dto;

import com.zhixiaojiang.common.constant.WarningStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 预警研判与关闭请求。
 *
 * <p>{@code expectedStatus} 是教师读到记录时的状态，用于拒绝基于过期状态的提交；
 * {@code escalate} 表示是否升级为需要人工重点研判。
 */
public record WarningReviewRequest(@NotBlank @Size(max = 400) String note,
                                   @NotBlank @Pattern(regexp = WarningStatus.PATTERN_TRIAGE) String expectedStatus,
                                   boolean escalate) {
}
