package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.service.AiAnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** AI 辅助分析：仅送匿名白名单字段，返回值固定附带免责声明与教师确认要求。 */
@RestController
@RequestMapping("/api/v1")
public class AiAnalysisController {
    private final AiAnalysisService ai;

    public AiAnalysisController(AiAnalysisService ai) {
        this.ai = ai;
    }

    @PostMapping("/ai/student-analysis")
    Map<String, Object> analyze(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(ai.analyze(body));
    }
}
