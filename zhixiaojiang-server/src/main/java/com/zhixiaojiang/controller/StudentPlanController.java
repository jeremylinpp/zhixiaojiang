package com.zhixiaojiang.controller;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.service.StudentPlanService;
import com.zhixiaojiang.service.StudentMessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/v1")
public class StudentPlanController {
 private final StudentPlanService plans;private final StudentMessageService messages;
 public StudentPlanController(StudentPlanService plans,StudentMessageService messages){this.plans=plans;this.messages=messages;}
 @GetMapping("/student-portal/plans") public Map<String,Object> list(){return ApiResult.ok(plans.list());}
 @GetMapping("/student-portal/plans/{id}") public Map<String,Object> detail(@PathVariable long id){return ApiResult.ok(plans.detail(id,false));}
 @PostMapping("/student-portal/plans/{id}/executions") public Map<String,Object> execute(@PathVariable long id,@Valid @RequestBody StudentPlanService.Execution body){return ApiResult.ok(plans.execute(id,body));}
 @GetMapping("/interventions/{id}/student-publications") public Map<String,Object> publications(@PathVariable long id){return ApiResult.ok(plans.teacherView(id));}
 @PostMapping("/interventions/{id}/student-publications") public Map<String,Object> publish(@PathVariable long id,@Valid @RequestBody StudentPlanService.Publication body){return ApiResult.ok(plans.publish(id,body));}
 @GetMapping("/student-plan-publications/{id}") public Map<String,Object> teacherDetail(@PathVariable long id){return ApiResult.ok(plans.detail(id,true));}
 @PostMapping("/student-plan-publications/{id}/feedback") public Map<String,Object> feedback(@PathVariable long id,@Valid @RequestBody StudentPlanService.Feedback body){return ApiResult.ok(plans.feedback(id,body));}
 @GetMapping("/student-portal/messages") public Map<String,Object> messages(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize){return ApiResult.ok(messages.list(page,pageSize));}
 @PostMapping("/student-portal/messages/{id}/read") public Map<String,Object> read(@PathVariable long id){return ApiResult.ok(messages.read(id));}
}
