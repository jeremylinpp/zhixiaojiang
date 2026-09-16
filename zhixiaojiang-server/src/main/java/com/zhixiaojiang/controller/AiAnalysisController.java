package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.model.dto.AiAnalysisRequest;
import com.zhixiaojiang.service.AiAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** AI 辅助分析：只提交学生 id，送模型的数据由服务端按白名单组装。 */
@RestController
@RequestMapping("/api/v1")
public class AiAnalysisController {
    private final AiAnalysisService ai;

    public AiAnalysisController(AiAnalysisService ai) {
        this.ai = ai;
    }

    @PostMapping("/ai/student-analysis")
    Map<String, Object> analyze(@Valid @RequestBody AiAnalysisRequest body) {
        return ApiResult.ok(ai.analyze(body.studentId()));
    }
}
