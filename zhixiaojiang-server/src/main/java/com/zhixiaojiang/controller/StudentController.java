package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.model.dto.StudentRequest;
import com.zhixiaojiang.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 学生档案：列表、详情、成长相关的只读视图，以及新增、修改与归档。 */
@RestController
@RequestMapping("/api/v1")
public class StudentController {
    private final StudentService students;

    public StudentController(StudentService students) {
        this.students = students;
    }

    @GetMapping("/students")
    Map<String, Object> list(@RequestParam(defaultValue = "") String q,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResult.ok(students.page(q, page, pageSize));
    }

    @GetMapping("/students/{id}")
    Map<String, Object> detail(@PathVariable long id) {
        return ApiResult.ok(students.detail(id));
    }

    @GetMapping("/students/{id}/portrait")
    Map<String, Object> portrait(@PathVariable long id) {
        return ApiResult.ok(students.portrait(id));
    }

    @GetMapping("/students/{id}/timeline")
    Map<String, Object> timeline(@PathVariable long id) {
        return ApiResult.ok(students.timeline(id));
    }

    @GetMapping("/students/{id}/attendance")
    Map<String, Object> attendance(@PathVariable long id) {
        return ApiResult.ok(students.attendance(id));
    }

    @GetMapping("/students/{id}/behavior")
    Map<String, Object> behavior(@PathVariable long id) {
        return ApiResult.ok(students.behavior(id));
    }

    @PostMapping("/students/{id}/behavior")
    Map<String, Object> recordBehavior(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(students.recordBehavior(id, body));
    }

    @GetMapping("/students/{id}/skills")
    Map<String, Object> skills(@PathVariable long id) {
        return ApiResult.ok(students.skills(id));
    }

    @GetMapping("/students/{id}/evaluations")
    Map<String, Object> evaluations(@PathVariable long id) {
        return ApiResult.ok(students.evaluations(id));
    }

    @PostMapping("/students")
    Map<String, Object> create(@Valid @RequestBody StudentRequest body) {
        return ApiResult.ok(students.create(body));
    }

    @PutMapping("/students/{id}")
    Map<String, Object> update(@PathVariable long id, @Valid @RequestBody StudentRequest body) {
        return ApiResult.ok(students.update(id, body));
    }

    @PostMapping("/students/{id}/archive")
    Map<String, Object> archive(@PathVariable long id) {
        return ApiResult.ok(students.archive(id));
    }
}
