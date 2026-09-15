package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.service.ClassDiagnosisService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 班级诊改：目标与偏差、改进措施记录、复评。 */
@RestController
@RequestMapping("/api/v1")
public class ClassDiagnosisController {
    private final ClassDiagnosisService diagnoses;

    public ClassDiagnosisController(ClassDiagnosisService diagnoses) {
        this.diagnoses = diagnoses;
    }

    @GetMapping("/class-diagnoses")
    Map<String, Object> list() {
        return ApiResult.ok(diagnoses.list());
    }

    @GetMapping("/class-diagnoses/{id}/records")
    Map<String, Object> records(@PathVariable long id) {
        return ApiResult.ok(diagnoses.records(id));
    }

    @PostMapping("/class-diagnoses/{id}/records")
    Map<String, Object> addRecord(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(diagnoses.addRecord(id, body));
    }

    @PostMapping("/class-diagnoses")
    Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return ApiResult.ok(diagnoses.create(body));
    }

    @PutMapping("/class-diagnoses/{id}")
    Map<String, Object> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(diagnoses.update(id, body));
    }

    @PostMapping("/class-diagnoses/{id}/review")
    Map<String, Object> review(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResult.ok(diagnoses.review(id, body));
    }
}
