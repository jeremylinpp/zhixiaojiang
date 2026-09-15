package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 班级数字驾驶舱。 */
@RestController
@RequestMapping("/api/v1")
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard/class")
    Map<String, Object> dashboard() {
        return ApiResult.ok(dashboard.classOverview());
    }
}
