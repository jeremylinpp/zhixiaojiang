package com.zhixiaojiang;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.common.constant.AttendanceStatus;
import com.zhixiaojiang.common.constant.GrowthDimension;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/v1/students/{studentId}")
public class GrowthWorkspaceController {
    private final JdbcTemplate db;
    private final TeacherScope scope;
    public GrowthWorkspaceController(JdbcTemplate db, TeacherScope scope) { this.db = db; this.scope = scope; }
    public record Exam(@NotBlank @Size(max=40) String subject,
                       @NotBlank @Size(max=120) String examName,
                       @NotNull @DecimalMin("0") @DecimalMax("9999.99") BigDecimal score,
                       @NotNull @DecimalMin("0.01") @DecimalMax("9999.99") BigDecimal fullScore,
                       @NotNull @PastOrPresent LocalDate occurredOn) {}
    public record Evaluation(@NotNull @PastOrPresent LocalDate periodStart,
                             @NotNull @PastOrPresent LocalDate periodEnd,
                             @DecimalMin("0") @DecimalMax("100") BigDecimal moralScore,
                             @DecimalMin("0") @DecimalMax("100") BigDecimal skillScore,
                             @DecimalMin("0") @DecimalMax("100") BigDecimal thinkingScore,
                             @DecimalMin("0") @DecimalMax("100") BigDecimal smartScore,
                             @NotBlank @Size(max=1000) String evidence) {}
    public record Attendance(@NotNull @PastOrPresent LocalDate attendanceDate,
                             @NotBlank @Pattern(regexp=AttendanceStatus.PATTERN) String status,
                             @Size(max=200) String note) {}
    public record Skill(@NotBlank @Size(max=120) String skillName,
                        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal score,
                        @Size(max=32) String level,@NotNull @PastOrPresent LocalDate occurredOn,
                        @NotBlank @Size(max=500) String evidence) {}
    public record Growth(@NotBlank @Pattern(regexp=GrowthDimension.PATTERN) String dimension,
                         @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal score,
                         @NotBlank @Size(max=160) String title,@Size(max=500) String detail,
                         @NotNull @PastOrPresent LocalDate occurredOn,@NotBlank @Size(max=64) String source) {}

    @GetMapping("/growth-workspace")
    Map<String,Object> workspace(@PathVariable long studentId,
                               @RequestParam(required=false) LocalDate from,
                               @RequestParam(required=false) LocalDate to,
                               HttpServletRequest request) {
        scope.requireStudent(studentId, request, false);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfYear(1) : from;
        if (start.isAfter(end)) throw bad("开始日期不能晚于结束日期");
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("student", rows("select id,name,student_no,status from student where id=?",studentId).get(0));
        data.put("from",start.toString()); data.put("to",end.toString());
        var dimensions = db.queryForMap("select avg(moral_score) moral,avg(skill_score) skill,avg(thinking_score) thinking,avg(smart_score) smart from dimension_evaluation where student_id=? and period_end between ? and ?",studentId,start,end);
        Map<String,Object> averages = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO; boolean complete = true;
        for (String key : List.of("moral","skill","thinking","smart")) {
            Object value = dimensions.get(key);
            if(value == null) { complete = false; averages.put(key,null); }
            else { BigDecimal decimal = new BigDecimal(value.toString()); sum=sum.add(decimal); averages.put(key,decimal.setScale(1,RoundingMode.HALF_UP)); }
        }
        data.put("dimensions",averages);
        data.put("growthIndex",complete ? sum.divide(BigDecimal.valueOf(4),1,RoundingMode.HALF_UP) : null);
        data.put("scores",rows("select id,subject,exam_name,score,full_score,occurred_on,created_by from score_record where student_id=? and occurred_on between ? and ? order by occurred_on desc,id desc limit 100",studentId,start,end));
        data.put("evaluations",rows("select id,period_start,period_end,moral_score,skill_score,thinking_score,smart_score,evidence,created_by from dimension_evaluation where student_id=? and period_end between ? and ? order by period_end desc,id desc limit 100",studentId,start,end));
        data.put("growth",rows("select id,dimension,score,title,detail,occurred_on,source,created_by from growth_record where student_id=? and occurred_on between ? and ? order by occurred_on desc,id desc limit 100",studentId,start,end));
        data.put("attendance",rows("select id,attendance_date,status,note,created_by from attendance_record where student_id=? and attendance_date between ? and ? order by attendance_date desc limit 100",studentId,start,end));
        data.put("skills",rows("select id,skill_name,score,level,occurred_on,evidence,created_by from skill_record where student_id=? and occurred_on between ? and ? order by occurred_on desc,id desc limit 100",studentId,start,end));
        return ApiResult.ok(data);
    }

    @PostMapping("/scores") @Transactional
    Map<String,Object> createExam(@PathVariable long studentId,@Valid @RequestBody Exam exam,HttpServletRequest request) {
        long actor=scope.requireStudent(studentId,request,true);
        if(exam.score().compareTo(exam.fullScore())>0) throw bad("成绩不能超过满分");
        if(db.queryForObject("select count(*) from score_record where student_id=? and subject=? and exam_name=?",Integer.class,studentId,exam.subject().trim(),exam.examName().trim())>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"该学生已有同科目、同考试批次成绩，请勿重复录入");
        long id=insert("insert into score_record(student_id,subject,exam_name,score,full_score,occurred_on,created_by) values(?,?,?,?,?,?,?)",studentId,exam.subject().trim(),exam.examName().trim(),exam.score(),exam.fullScore(),exam.occurredOn(),actor);
        audit(actor,"score_record",id,"教师录入考试成绩"); return ApiResult.ok(Map.of("id",id));
    }

    @PostMapping("/attendance") @Transactional
    Map<String,Object> attendance(@PathVariable long studentId,@Valid @RequestBody Attendance attendance,HttpServletRequest request) {
        long actor=scope.requireStudent(studentId,request,true);
        var existing=db.queryForList("select id from attendance_record where student_id=? and attendance_date=?",Long.class,studentId,attendance.attendanceDate());
        long id;
        if(existing.isEmpty()) id=insert("insert into attendance_record(student_id,attendance_date,status,note,created_by) values(?,?,?,?,?)",studentId,attendance.attendanceDate(),attendance.status(),attendance.note(),actor);
        else {id=existing.get(0);db.update("update attendance_record set status=?,note=?,created_by=? where id=?",attendance.status(),attendance.note(),actor,id);}
        audit(actor,"attendance_record",id,"教师登记出勤："+attendance.status());return ApiResult.ok(Map.of("saved",true,"id",id));
    }
    @PostMapping("/skills") @Transactional
    Map<String,Object> skill(@PathVariable long studentId,@Valid @RequestBody Skill skill,HttpServletRequest request) {
        long actor=scope.requireStudent(studentId,request,true);
        long id=insert("insert into skill_record(student_id,skill_name,score,level,occurred_on,evidence,created_by) values(?,?,?,?,?,?,?)",studentId,skill.skillName().trim(),skill.score(),skill.level(),skill.occurredOn(),skill.evidence().trim(),actor);
        audit(actor,"skill_record",id,"教师录入技能记录");return ApiResult.ok(Map.of("id",id));
    }
    @PostMapping("/growth") @Transactional
    Map<String,Object> growth(@PathVariable long studentId,@Valid @RequestBody Growth growth,HttpServletRequest request) {
        long actor=scope.requireStudent(studentId,request,true);
        long id=insert("insert into growth_record(student_id,dimension,score,title,detail,occurred_on,source,created_by) values(?,?,?,?,?,?,?,?)",studentId,growth.dimension(),growth.score(),growth.title().trim(),growth.detail(),growth.occurredOn(),growth.source().trim(),actor);
        audit(actor,"growth_record",id,"教师录入成长记录");return ApiResult.ok(Map.of("id",id));
    }

    @PostMapping("/evaluations") @Transactional
    Map<String,Object> createEvaluation(@PathVariable long studentId,@Valid @RequestBody Evaluation evaluation,HttpServletRequest request) {
        long actor=scope.requireStudent(studentId,request,true);
        if(evaluation.periodStart().isAfter(evaluation.periodEnd())) throw bad("评价周期开始日期不能晚于结束日期");
        if(evaluation.moralScore()==null && evaluation.skillScore()==null && evaluation.thinkingScore()==null && evaluation.smartScore()==null) throw bad("请至少填写一个维度的评价分数");
        long id=insert("insert into dimension_evaluation(student_id,period_start,period_end,moral_score,skill_score,thinking_score,smart_score,evidence,created_by) values(?,?,?,?,?,?,?,?,?)",studentId,evaluation.periodStart(),evaluation.periodEnd(),evaluation.moralScore(),evaluation.skillScore(),evaluation.thinkingScore(),evaluation.smartScore(),evaluation.evidence().trim(),actor);
        audit(actor,"dimension_evaluation",id,"教师录入四维评价"); return ApiResult.ok(Map.of("id",id));
    }

    private List<Map<String,Object>> rows(String sql,Object... args) {
        return db.query(sql,(rs,n)->{
            Map<String,Object> row=new LinkedHashMap<>();
            var metadata=rs.getMetaData();
            for(int column=1;column<=metadata.getColumnCount();column++){
                String[] words=metadata.getColumnLabel(column).toLowerCase(Locale.ROOT).split("_");
                StringBuilder key=new StringBuilder(words[0]);
                for(int i=1;i<words.length;i++) key.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
                Object value=rs.getObject(column);
                row.put(key.toString(),value instanceof java.sql.Date ? value.toString() : value);
            }
            return row;
        },args);
    }
    private long insert(String sql,Object... values) {
        var keys=new GeneratedKeyHolder();
        db.update(connection->{var statement=connection.prepareStatement(sql,new String[]{"id"});for(int i=0;i<values.length;i++)statement.setObject(i+1,values[i]);return statement;},keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }
    private void audit(long actor,String entity,long id,String summary){db.update("insert into audit_log(actor_id,action,entity_type,entity_id,summary) values(?,'CREATE',?,?,?)",actor,entity,id,summary);}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
