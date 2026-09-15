package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.model.dto.PointAwardRequest;
import com.zhixiaojiang.service.PointsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 机智币：流水与余额、发放、撤销纠错、积分规则。 */
@RestController
@RequestMapping("/api/v1")
public class PointsController {
    private final PointsService points;

    public PointsController(PointsService points) {
        this.points = points;
    }

    @GetMapping("/students/{id}/points")
    Map<String, Object> ledger(@PathVariable long id,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResult.ok(points.ledger(id, page, pageSize));
    }

    @PostMapping("/points")
    Map<String, Object> award(@Valid @RequestBody PointAwardRequest body) {
        return ApiResult.ok(points.award(body));
    }

    @PostMapping("/points/{id}/reverse")
    Map<String, Object> reverse(@PathVariable long id) {
        return ApiResult.ok(points.reverse(id));
    }

    @GetMapping("/point-rules")
    Map<String, Object> rules() {
        return ApiResult.ok(points.rules());
    }

    @PostMapping("/point-rules")
    Map<String, Object> createRule(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(points.createRule(body));
    }
}
