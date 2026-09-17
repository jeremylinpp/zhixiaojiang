package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.dao.StudentPlanMapper;
import com.zhixiaojiang.model.po.StudentPlanExecution;
import com.zhixiaojiang.model.po.StudentPlanFeedback;
import com.zhixiaojiang.model.po.StudentPlanPublication;
import com.zhixiaojiang.model.vo.PortalPlanContext;
import com.zhixiaojiang.model.vo.PortalPlanExecutionRow;
import com.zhixiaojiang.model.vo.PortalPlanFeedbackRow;
import com.zhixiaojiang.model.vo.PortalPlanRow;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.Map;

@Service
public class StudentPlanService {
 private final StudentPlanMapper plans;private final StudentScope students;private final TeacherScope teachers;private final AuditRecorder audit;private final StudentMessageService messages;
 public StudentPlanService(StudentPlanMapper plans,StudentScope students,TeacherScope teachers,AuditRecorder audit,StudentMessageService messages){this.plans=plans;this.students=students;this.teachers=teachers;this.audit=audit;this.messages=messages;}
 public record Publication(@Min(0) int expectedVersion,@NotBlank @Size(max=160) String title,@NotBlank @Size(max=1000) String goal,@NotBlank @Size(max=2000) String actions,@NotNull LocalDate reviewOn){}
 public record Execution(@NotBlank @Size(max=80) String requestKey,@NotBlank @Size(max=2000) String content,@NotNull @PastOrPresent LocalDate occurredOn){}
 public record Feedback(@NotBlank @Size(max=80) String requestKey,@NotBlank @Size(max=2000) String content){}
 public Map<String,Object> list(){
  return Map.of("items",plans.visibleToStudent(students.studentId()));
 }
 public Map<String,Object> teacherView(long planId){teachers.requirePlan(planId,false);return Map.of("items",plans.versionsOfPlan(planId));}
 @Transactional
 public Map<String,Object> publish(long planId,Publication body){
  var plan=teachers.requirePlan(planId,true);
  if(!java.util.List.of("CONFIRMED","IN_PROGRESS").contains(plan.getStatus()))throw conflict("仅已确认或执行中的方案可发布给学生");
  int current=plans.latestVersion(planId);
  if(current!=body.expectedVersion())throw conflict("方案已发布或版本发生变化，请刷新后操作");
  StudentPlanPublication publication=new StudentPlanPublication();
  publication.setPlanId(planId);publication.setVersion(current+1);publication.setTitle(body.title().trim());
  publication.setGoal(body.goal().trim());publication.setActions(body.actions().trim());publication.setReviewOn(body.reviewOn());
  plans.insertPublication(publication);
  long id=publication.getId();
  messages.send(plan.getStudentId(),"plan:"+id,"成长计划已发布","教师为你发布了新的目标与行动安排，请查看。","计划");
  audit.record("PUBLISH_STUDENT_PLAN","student_plan_publication",id,"教师确认并发布学生可见版本");
  return Map.of("id",id,"version",current+1);
 }
 public Map<String,Object> detail(long publicationId,boolean teacher){
  var plan=require(publicationId,teacher,false);
  java.util.List<PortalPlanRow> versions=plans.versionsOfPlan(plan.getPlanId());
  return Map.of("versions",versions,"latestVersion",versions.get(0).getVersion(),"executions",plans.executions(publicationId),"feedback",plans.feedback(publicationId));
 }
 @Transactional
 public Map<String,Object> execute(long id,Execution body){
  var plan=require(id,false,true);
  var existing=plans.executionByRequestKey(id,body.requestKey());
  if(existing.isPresent()){
   PortalPlanExecutionRow row=existing.get();
   if(!body.content().trim().equals(row.getContent()) || !body.occurredOn().toString().equals(row.getOccurredOn().toString()))throw conflict("重复请求内容不一致");
   return Map.of("id",row.getId(),"saved",false);
  }
  if(!java.util.List.of("CONFIRMED","IN_PROGRESS").contains(plan.getStatus()))throw conflict("该计划已结束，不能新增执行记录");
  if(plans.latestVersion(plan.getPlanId())!=plan.getVersion().intValue())throw conflict("已有新版计划，请刷新后记录");
  StudentPlanExecution execution=new StudentPlanExecution();
  execution.setPublicationId(id);execution.setRequestKey(body.requestKey());execution.setContent(body.content().trim());execution.setOccurredOn(body.occurredOn());
  plans.insertExecution(execution);
  long record=execution.getId();
  audit.record("STUDENT_EXECUTE","student_plan_execution",record,"学生记录计划执行情况");
  return Map.of("id",record,"saved",true);
 }
 @Transactional
 public Map<String,Object> feedback(long id,Feedback body){
  var plan=require(id,true,true);
  var existing=plans.feedbackByRequestKey(id,body.requestKey());
  if(existing.isPresent()){
   PortalPlanFeedbackRow row=existing.get();
   if(!body.content().trim().equals(row.getContent()))throw conflict("重复请求内容不一致");
   return Map.of("id",row.getId(),"saved",false);
  }
  StudentPlanFeedback feedback=new StudentPlanFeedback();
  feedback.setPublicationId(id);feedback.setRequestKey(body.requestKey());feedback.setContent(body.content().trim());feedback.setCreatedBy(teachers.teacher());
  plans.insertFeedback(feedback);
  long record=feedback.getId();
  messages.send(plan.getStudentId(),"plan-feedback:"+record,"收到阶段反馈","教师更新了你的成长计划反馈，请查看。","计划");
  audit.record("STUDENT_PLAN_FEEDBACK","student_plan_feedback",record,"教师发布学生可见阶段反馈");
  return Map.of("id",record,"saved",true);
 }
 private PortalPlanContext require(long id,boolean teacher,boolean lock){
  var context=teacher?plans.contextForTeacher(id,teachers.teacher(),lock):plans.contextForStudent(id,students.studentId(),lock);
  return context.orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"成长计划不存在"));
 }
 private ResponseStatusException conflict(String reason){return new ResponseStatusException(HttpStatus.CONFLICT,reason);}
}
