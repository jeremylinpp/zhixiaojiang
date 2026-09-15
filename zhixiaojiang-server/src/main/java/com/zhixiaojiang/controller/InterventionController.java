package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.model.dto.InterventionRequest;
import com.zhixiaojiang.service.InterventionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 一人一策：方案列表与详情、创建、修订、状态流转、阶段复评与执行过程记录。 */
@RestController
@RequestMapping("/api/v1")
public class InterventionController {
    private final InterventionService plans;

    public InterventionController(InterventionService plans) {
        this.plans = plans;
    }

    @GetMapping("/interventions")
    Map<String, Object> list() {
        return ApiResult.ok(plans.list());
    }

    @PostMapping("/interventions")
    Map<String, Object> create(@Valid @RequestBody InterventionRequest body) {
        return ApiResult.ok(plans.create(body));
    }

    @GetMapping("/interventions/{id}")
    Map<String, Object> detail(@PathVariable long id) {
        return ApiResult.ok(plans.detail(id));
    }

    @PutMapping("/interventions/{id}")
    Map<String, Object> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(plans.update(id, body));
    }

    @PostMapping("/interventions/{id}/transition")
    Map<String, Object> transition(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(plans.transition(id, body.get("status") == null ? null : String.valueOf(body.get("status"))));
    }

    @PostMapping("/interventions/{id}/review")
    Map<String, Object> review(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(plans.review(id, body));
    }

    @PostMapping("/interventions/{id}/records")
    Map<String, Object> records(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(plans.addRecord(id, body));
    }
}
