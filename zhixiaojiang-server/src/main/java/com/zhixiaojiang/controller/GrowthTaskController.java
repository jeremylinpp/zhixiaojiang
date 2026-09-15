package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.service.GrowthTaskService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 六机成长任务：发布、指派、确认完成、评价。 */
@RestController
@RequestMapping("/api/v1")
public class GrowthTaskController {
    private final GrowthTaskService tasks;

    public GrowthTaskController(GrowthTaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping("/growth-tasks")
    Map<String, Object> list() {
        return ApiResult.ok(tasks.list());
    }

    @PostMapping("/growth-tasks")
    Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(tasks.create(body));
    }

    @PostMapping("/growth-tasks/{id}/assign")
    Map<String, Object> assign(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(tasks.assign(id, body.get("studentIds")));
    }

    @GetMapping("/growth-tasks/{id}/students")
    Map<String, Object> students(@PathVariable long id) {
        return ApiResult.ok(tasks.studentsOfTask(id));
    }

    @PostMapping("/student-tasks/{id}/complete")
    Map<String, Object> complete(@PathVariable long id, @RequestBody(required = false) Map<String, Object> body) {
        return ApiResult.ok(tasks.complete(id, body == null || body.get("note") == null ? null : String.valueOf(body.get("note"))));
    }

    @PostMapping("/student-tasks/{id}/evaluate")
    Map<String, Object> evaluate(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(tasks.evaluate(id, body.get("note") == null ? null : String.valueOf(body.get("note"))));
    }
}
