package com.zhixiaojiang.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskSubmissionRequest(
        @NotBlank @Size(max=80) String requestKey,
        @NotBlank @Size(max=4000) String content,
        @Size(max=3) java.util.List<@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long> attachmentIds) {}
