package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.model.dto.AttendanceRequest;
import com.zhixiaojiang.model.dto.EvaluationRequest;
import com.zhixiaojiang.model.dto.ExamRequest;
import com.zhixiaojiang.model.dto.GrowthRequest;
import com.zhixiaojiang.model.dto.SkillRequest;
import com.zhixiaojiang.service.GrowthWorkspaceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/** 成长工作台：按周期汇总四维评价、成绩、技能、成长记录与出勤，并提供五类录入。 */
@RestController
@RequestMapping("/api/v1/students/{studentId}")
public class GrowthWorkspaceController {
    private final GrowthWorkspaceService growth;

    public GrowthWorkspaceController(GrowthWorkspaceService growth) {
        this.growth = growth;
    }

    @GetMapping("/growth-workspace")
    Map<String, Object> workspace(@PathVariable long studentId,
                                  @RequestParam(required = false) LocalDate from,
                                  @RequestParam(required = false) LocalDate to) {
        return ApiResult.ok(growth.workspace(studentId, from, to));
    }

    @PostMapping("/scores")
    Map<String, Object> createExam(@PathVariable long studentId, @Valid @RequestBody ExamRequest body) {
        return ApiResult.ok(growth.recordExam(studentId, body));
    }

    @PostMapping("/attendance")
    Map<String, Object> attendance(@PathVariable long studentId, @Valid @RequestBody AttendanceRequest body) {
        return ApiResult.ok(growth.recordAttendance(studentId, body));
    }

    @PostMapping("/skills")
    Map<String, Object> skill(@PathVariable long studentId, @Valid @RequestBody SkillRequest body) {
        return ApiResult.ok(growth.recordSkill(studentId, body));
    }

    @PostMapping("/growth")
    Map<String, Object> growthRecord(@PathVariable long studentId, @Valid @RequestBody GrowthRequest body) {
        return ApiResult.ok(growth.recordGrowth(studentId, body));
    }

    @PostMapping("/evaluations")
    Map<String, Object> evaluation(@PathVariable long studentId, @Valid @RequestBody EvaluationRequest body) {
        return ApiResult.ok(growth.recordEvaluation(studentId, body));
    }
}
