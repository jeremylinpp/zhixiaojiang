package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.model.dto.WarningReviewRequest;
import com.zhixiaojiang.service.WarningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 智能预警：列表、详情、教师研判与关闭，以及规则筛查。 */
@RestController
@RequestMapping("/api/v1/warnings")
public class WarningController {
    private final WarningService warnings;

    public WarningController(WarningService warnings) {
        this.warnings = warnings;
    }

    @GetMapping
    Map<String, Object> list(@RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "") String q,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResult.ok(warnings.page(status, q, page, pageSize));
    }

    @GetMapping("/{id}")
    Map<String, Object> detail(@PathVariable long id) {
        return ApiResult.ok(warnings.detail(id));
    }

    @PostMapping("/{id}/triage")
    Map<String, Object> triage(@PathVariable long id, @Valid @RequestBody WarningReviewRequest body) {
        return ApiResult.ok(warnings.triage(id, body));
    }

    @PostMapping("/{id}/close")
    Map<String, Object> close(@PathVariable long id, @Valid @RequestBody WarningReviewRequest body) {
        return ApiResult.ok(warnings.close(id, body));
    }

    @PostMapping("/analyze")
    Map<String, Object> analyze() {
        return ApiResult.ok(warnings.analyzeRules());
    }
}
