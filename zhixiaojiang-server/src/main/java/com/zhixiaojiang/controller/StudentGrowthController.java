package com.zhixiaojiang.controller;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.service.StudentGrowthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class StudentGrowthController {
    private final StudentGrowthService service;
    public StudentGrowthController(StudentGrowthService service){this.service=service;}
    @GetMapping("/student-portal/growth")
    public Map<String,Object> growth(@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){return ApiResult.ok(service.workspace(from,to));}
    @PostMapping("/student-portal/growth")
    public Map<String,Object> submit(@Valid @RequestBody StudentGrowthService.Submission body){return ApiResult.ok(service.submit(body));}
    @GetMapping("/students/{id}/growth-submissions")
    public Map<String,Object> pending(@PathVariable long id){return ApiResult.ok(service.pending(id));}
    @PostMapping("/growth-submissions/{id}/review")
    public Map<String,Object> review(@PathVariable long id,@Valid @RequestBody StudentGrowthService.Review body){return ApiResult.ok(service.review(id,body));}
}
