package com.zhixiaojiang.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 学生档案的新增与修改请求；classId 缺省时落到教师负责的第一个班级。 */
public record StudentRequest(@NotBlank @Size(max = 80) String name,
                             @NotBlank @Size(max = 32) String studentNo,
                             @Size(max = 16) String gender,
                             Long classId) {
}
