package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RowMaps;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.Map;

@Service
public class StudentPlanService {
 private final JdbcTemplate db;private final StudentScope students;private final TeacherScope teachers;private final AuditRecorder audit;private final StudentMessageService messages;
 public StudentPlanService(JdbcTemplate db,StudentScope students,TeacherScope teachers,AuditRecorder audit,StudentMessageService messages){this.db=db;this.students=students;this.teachers=teachers;this.audit=audit;this.messages=messages;}
 public record Publication(@Min(0) int expectedVersion,@NotBlank @Size(max=160) String title,@NotBlank @Size(max=1000) String goal,@NotBlank @Size(max=2000) String actions,@NotNull LocalDate reviewOn){}
 public record Execution(@NotBlank @Size(max=80) String requestKey,@NotBlank @Size(max=2000) String content,@NotNull @PastOrPresent LocalDate occurredOn){}
 public record Feedback(@NotBlank @Size(max=80) String requestKey,@NotBlank @Size(max=2000) String content){}
 public Map<String,Object> list(){
  return Map.of("items",db.query("select p.id,p.title,p.goal,p.actions,p.review_on,p.version,i.status from student_plan_publication p join intervention_plan i on i.id=p.plan_id where i.student_id=? and i.status<>'DRAFT' and not exists(select 1 from student_plan_publication n where n.plan_id=p.plan_id and n.version>p.version) order by p.id desc",RowMaps.mapper(),students.studentId()));
 }
 public Map<String,Object> teacherView(long planId){teachers.requirePlan(planId,false);return Map.of("items",db.query("select id,title,goal,actions,review_on,version from student_plan_publication where plan_id=? order by version desc",RowMaps.mapper(),planId));}
 @Transactional
 public Map<String,Object> publish(long planId,Publication body){
  var plan=teachers.requirePlan(planId,true);
  if(!java.util.List.of("CONFIRMED","IN_PROGRESS").contains(plan.getStatus()))throw conflict("仅已确认或执行中的方案可发布给学生");
  int current=db.queryForObject("select coalesce(max(version),0) from student_plan_publication where plan_id=?",Integer.class,planId);
  if(current!=body.expectedVersion())throw conflict("方案已发布或版本发生变化，请刷新后操作");
  long id=JdbcInsert.returningId(db,"insert into student_plan_publication(plan_id,version,title,goal,actions,review_on) values(?,?,?,?,?,?)",planId,current+1,body.title().trim(),body.goal().trim(),body.actions().trim(),body.reviewOn());
  messages.send(plan.getStudentId(),"plan:"+id,"成长计划已发布","教师为你发布了新的目标与行动安排，请查看。","计划");
  audit.record("PUBLISH_STUDENT_PLAN","student_plan_publication",id,"教师确认并发布学生可见版本");
  return Map.of("id",id,"version",current+1);
 }
 public Map<String,Object> detail(long publicationId,boolean teacher){
  var plan=require(publicationId,teacher,false);
  var versions=db.query("select p.id,p.title,p.goal,p.actions,p.review_on,p.version,i.status from student_plan_publication p join intervention_plan i on i.id=p.plan_id where p.plan_id=? order by p.version desc",RowMaps.mapper(),plan.get("planId"));
  return Map.of("versions",versions,"latestVersion",versions.get(0).get("version"),"executions",db.query("select id,content,occurred_on,created_at from student_plan_execution where publication_id=? order by id desc",RowMaps.mapper(),publicationId),"feedback",db.query("select id,content,created_at from student_plan_feedback where publication_id=? order by id desc",RowMaps.mapper(),publicationId));
 }
 @Transactional
 public Map<String,Object> execute(long id,Execution body){
  var plan=require(id,false,true);
  var existing=db.query("select id,content,occurred_on from student_plan_execution where publication_id=? and request_key=?",RowMaps.mapper(),id,body.requestKey());
  if(!existing.isEmpty()){
   if(!body.content().trim().equals(existing.get(0).get("content")) || !body.occurredOn().toString().equals(existing.get(0).get("occurredOn").toString()))throw conflict("重复请求内容不一致");
   return Map.of("id",existing.get(0).get("id"),"saved",false);
  }
  if(!java.util.List.of("CONFIRMED","IN_PROGRESS").contains(plan.get("status")))throw conflict("该计划已结束，不能新增执行记录");
  int latest=db.queryForObject("select max(version) from student_plan_publication where plan_id=?",Integer.class,plan.get("planId"));
  if(latest!=((Number)plan.get("version")).intValue())throw conflict("已有新版计划，请刷新后记录");
  long record=JdbcInsert.returningId(db,"insert into student_plan_execution(publication_id,request_key,content,occurred_on) values(?,?,?,?)",id,body.requestKey(),body.content().trim(),body.occurredOn());
  audit.record("STUDENT_EXECUTE","student_plan_execution",record,"学生记录计划执行情况");
  return Map.of("id",record,"saved",true);
 }
 @Transactional
 public Map<String,Object> feedback(long id,Feedback body){
  var plan=require(id,true,true);
  var existing=db.query("select id,content from student_plan_feedback where publication_id=? and request_key=?",RowMaps.mapper(),id,body.requestKey());
  if(!existing.isEmpty()){
   if(!body.content().trim().equals(existing.get(0).get("content")))throw conflict("重复请求内容不一致");
   return Map.of("id",existing.get(0).get("id"),"saved",false);
  }
  long record=JdbcInsert.returningId(db,"insert into student_plan_feedback(publication_id,request_key,content,created_by) values(?,?,?,?)",id,body.requestKey(),body.content().trim(),teachers.teacher());
  messages.send(((Number)plan.get("studentId")).longValue(),"plan-feedback:"+record,"收到阶段反馈","教师更新了你的成长计划反馈，请查看。","计划");
  audit.record("STUDENT_PLAN_FEEDBACK","student_plan_feedback",record,"教师发布学生可见阶段反馈");
  return Map.of("id",record,"saved",true);
 }
 private Map<String,Object> require(long id,boolean teacher,boolean lock){
  String condition=teacher?"c.teacher_id=?":"i.student_id=? and i.status<>'DRAFT'";
  return db.query("select p.plan_id,p.version,i.student_id,i.status from student_plan_publication p join intervention_plan i on i.id=p.plan_id join student s on s.id=i.student_id join class_room c on c.id=s.class_id where p.id=? and "+condition+(lock?" for update":""),RowMaps.mapper(),id,teacher?teachers.teacher():students.studentId()).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"成长计划不存在"));
 }
 private ResponseStatusException conflict(String reason){return new ResponseStatusException(HttpStatus.CONFLICT,reason);}
}
