package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.service.StudentPortalService;
import com.zhixiaojiang.service.TaskSubmissionService;
import com.zhixiaojiang.model.dto.TaskSubmissionRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/student-portal")
public class StudentPortalController {
    private final StudentPortalService service;
    private final TaskSubmissionService tasks;
    public StudentPortalController(StudentPortalService service, TaskSubmissionService tasks) { this.service = service; this.tasks=tasks; }

    @GetMapping("/tasks")
    public Map<String,Object> tasks() { return ApiResult.ok(tasks.list()); }

    @GetMapping("/tasks/{id}/submissions")
    public Map<String,Object> submissions(@PathVariable long id) { return ApiResult.ok(tasks.history(id,false)); }

    @PostMapping("/tasks/{id}/submissions")
    public Map<String,Object> submit(@PathVariable long id, @Valid @RequestBody TaskSubmissionRequest body) {
        return ApiResult.ok(tasks.submit(id,body));
    }

    @GetMapping("/home")
    public Map<String, Object> home() { return ApiResult.ok(service.home()); }

    @GetMapping("/points")
    public Map<String, Object> points(@RequestParam(defaultValue="1") int page,
                                      @RequestParam(defaultValue="20") int pageSize) {
        return ApiResult.ok(service.points(page, pageSize));
    }
}
