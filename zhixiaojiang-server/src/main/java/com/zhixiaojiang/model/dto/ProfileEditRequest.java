package com.zhixiaojiang.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 教师显示名称修改请求；previousDisplayName 用于乐观并发校验。 */
public record ProfileEditRequest(@NotBlank @Size(max = 80) String displayName,
                                 @NotBlank @Size(max = 80) String previousDisplayName) {
}
