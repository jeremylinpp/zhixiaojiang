package com.zhixiaojiang.model.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** 帮扶方案创建请求；warningId 用于标记方案来自哪条预警。 */
public record InterventionRequest(@NotNull Long studentId,
                                  Long warningId,
                                  String title,
                                  List<String> suggestions,
                                  String teacherNote,
                                  LocalDate reviewAt) {
}
