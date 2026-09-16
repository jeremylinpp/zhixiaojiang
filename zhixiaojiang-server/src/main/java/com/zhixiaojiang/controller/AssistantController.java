package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.service.AssistantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 智小匠助手：能力状态、已有数据量与使用边界。 */
@RestController
@RequestMapping("/api/v1")
public class AssistantController {
    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @GetMapping("/assistant/status")
    Map<String, Object> status() {
        return ApiResult.ok(assistant.status());
    }
}
