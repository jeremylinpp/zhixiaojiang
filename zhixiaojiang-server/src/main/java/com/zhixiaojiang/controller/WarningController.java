package com.zhixiaojiang.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.AttendanceStatus;
import com.zhixiaojiang.common.constant.StudentStatus;
import com.zhixiaojiang.common.constant.StudentTaskStatus;
import com.zhixiaojiang.common.constant.WarningRule;
import com.zhixiaojiang.common.util.JsonValues;
import com.zhixiaojiang.common.constant.WarningLevel;
import com.zhixiaojiang.common.constant.WarningStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.*;

@RestController @RequestMapping("/api/v1/warnings")
public class WarningController {
    /** 预警列表的“全部状态”筛选值，仅用于查询，不属于 WarningStatus 的取值域。 */
    private static final String STATUS_ALL = "ALL";

    private final JdbcTemplate db; private final ObjectMapper json;
    private final TeacherScope scope; private final AuditRecorder audit;

    public WarningController(JdbcTemplate db, ObjectMapper json, TeacherScope scope, AuditRecorder audit) {
        this.db = db;
        this.json = json;
        this.scope = scope;
        this.audit = audit;
    }
    public record Review(@NotBlank @Size(max=400) String note,
                         @NotBlank @Pattern(regexp=WarningStatus.PATTERN_TRIAGE) String expectedStatus,
                         boolean escalate) {}
    private static final String SELECT="select w.*,s.name student_name,s.student_no from warning_record w join student s on s.id=w.student_id join class_room c on c.id=s.class_id ";
    @GetMapping Map<String,Object> list(@RequestParam(required=false) String status,
        @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int page,
        @RequestParam(defaultValue="20") int pageSize,HttpServletRequest request){
        boolean all=STATUS_ALL.equals(status);
        WarningStatus requested=status==null||status.isBlank()?WarningStatus.OPEN:WarningStatus.of(status);
        if(!all&&requested==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"未知预警状态");
        String filter=all?STATUS_ALL:requested.name();
        int current=Math.max(1,page),size=Math.max(1,Math.min(100,pageSize));
        String where="where c.teacher_id=? and (?='"+STATUS_ALL+"' or w.status=?) and (s.name like ? or s.student_no like ? or w.summary like ?)";
        String like="%"+q.trim()+"%";long actor=actor(request);
        var items=db.query(SELECT+where+" order by w.id desc limit ? offset ?",(rs,n)->row(rs),actor,filter,filter,like,like,like,size,(current-1)*size);
        Long total=db.queryForObject("select count(*) from warning_record w join student s on s.id=w.student_id join class_room c on c.id=s.class_id "+where,Long.class,actor,filter,filter,like,like,like);
        return ApiResult.ok(Map.of("items",items,"total",total,"page",current,"pageSize",size));
    }
    @GetMapping("/{id}") Map<String,Object> detail(@PathVariable long id,HttpServletRequest request){
        var warning=owned(id,request,false);
        var events=db.query("select a.id,a.action,a.summary,a.created_at,u.display_name from audit_log a left join sys_user u on u.id=a.actor_id where a.entity_type='warning_record' and a.entity_id=? and a.action in ('TRIAGE','CLOSE') order by a.id",(rs,n)->Map.of("id",rs.getLong("id"),"action",rs.getString("action"),"note",rs.getString("summary"),"createdAt",rs.getTimestamp("created_at").toString(),"actor",Objects.toString(rs.getString("display_name"),"教师")),id);
        return ApiResult.ok(Map.of("warning",warning,"events",events));
    }
    @PostMapping("/{id}/triage") @Transactional
    Map<String,Object> triage(@PathVariable long id,@Valid @RequestBody Review review,HttpServletRequest request){
        var warning=owned(id,request,true);checkStatus(warning,review);
        if(!WarningStatus.OPEN.name().equals(warning.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"该预警已研判，不能重复提交");
        db.update("update warning_record set status=?,teacher_note=?,level=? where id=?",WarningStatus.REVIEWED.name(),review.note().trim(),review.escalate()?WarningLevel.MANUAL.name():warning.get("level"),id);
        audit.record(actor(request),"TRIAGE","warning_record",id,"教师研判："+review.note().trim());return ApiResult.ok(Map.of("saved",true,"status",WarningStatus.REVIEWED.name()));
    }
    @PostMapping("/{id}/close") @Transactional
    Map<String,Object> close(@PathVariable long id,@Valid @RequestBody Review review,HttpServletRequest request){
        var warning=owned(id,request,true);checkStatus(warning,review);
        db.update("update warning_record set status=?,closed_at=now() where id=?",WarningStatus.CLOSED.name(),id);
        audit.record(actor(request),"CLOSE","warning_record",id,"关闭原因："+review.note().trim());return ApiResult.ok(Map.of("saved",true,"status",WarningStatus.CLOSED.name()));
    }
    @PostMapping("/analyze")
    @Transactional
    Map<String, Object> analyze(HttpServletRequest req) {
        int created = 0;
        LocalDate today = LocalDate.now(), since = today.minusDays(13), priorSince = today.minusDays(27);
        for (Map<String, Object> s : db.queryForList("select s.id from student s join class_room c on c.id=s.class_id where c.teacher_id=? and s.status=?", scope.teacher(), StudentStatus.ACTIVE.name())) {
            long sid = ((Number) s.get("id")).longValue();
            List<Map<String, Object>> scores = db.queryForList("select score/full_score ratio,score from score_record where student_id=? and subject='数学' order by occurred_on desc limit 4", sid);
            boolean decline = scores.size() >= 4 && ((Number) scores.get(3).get("ratio")).doubleValue() > ((Number) scores.get(2).get("ratio")).doubleValue() && ((Number) scores.get(2).get("ratio")).doubleValue() > ((Number) scores.get(1).get("ratio")).doubleValue() && ((Number) scores.get(1).get("ratio")).doubleValue() > ((Number) scores.get(0).get("ratio")).doubleValue();
            boolean low = scores.size() >= 2 && scores.stream().limit(2).allMatch(x -> ((Number) x.get("ratio")).doubleValue() < 0.6);
            if (decline || low) {
                int n = db.update("insert ignore into warning_record(student_id,level,rule_code,summary,evidence_json,status) values(?,?,?,?,?,?)", sid, decline ? WarningLevel.FOCUS.name() : WarningLevel.ATTENTION.name(), decline ? WarningRule.SCORE_DECLINE.name() : WarningRule.SCORE_LOW.name(), decline ? "同科目最近四次考试连续下降" : "最近两次考试低于及格线", JsonValues.toJson(List.of("数学成绩趋势由规则引擎计算")), WarningStatus.OPEN.name());
                created += n;
            }
            Integer late = db.queryForObject("select count(*) from attendance_record where student_id=? and status=? and attendance_date>=?", Integer.class, sid, AttendanceStatus.LATE.name(), since);
            if (late != null && late >= 3) {
                created += db.update("insert ignore into warning_record(student_id,level,rule_code,summary,evidence_json,status) values(?,?,?,?,?,?)", sid, WarningLevel.ATTENTION.name(), WarningRule.LATE_14D.name(), "最近14天迟到至少3次", JsonValues.toJson(List.of("迟到次数=" + late)), WarningStatus.OPEN.name());
            }
            Integer overdue = db.queryForObject("select count(*) from student_task st join growth_task gt on gt.id=st.task_id where st.student_id=? and gt.due_on<? and st.status<>?", Integer.class, sid, today, StudentTaskStatus.COMPLETED.name());
            if (overdue != null && overdue >= 2) {
                created += db.update("insert ignore into warning_record(student_id,level,rule_code,summary,evidence_json,status) values(?,?,?,?,?,?)", sid, WarningLevel.ATTENTION.name(), WarningRule.TASK_OVERDUE.name(), "至少2项到期任务未完成", JsonValues.toJson(List.of("未完成到期任务=" + overdue)), WarningStatus.OPEN.name());
            }
            Integer recentActivity = db.queryForObject("select count(*) from activity_record where student_id=? and activity_date>=?", Integer.class, sid, since);
            Integer priorActivity = db.queryForObject("select count(*) from activity_record where student_id=? and activity_date>=? and activity_date<?", Integer.class, sid, priorSince, since);
            if (priorActivity != null && priorActivity >= 2 && recentActivity != null && recentActivity * 2 <= priorActivity) {
                created += db.update("insert ignore into warning_record(student_id,level,rule_code,summary,evidence_json,status) values(?,?,?,?,?,?)", sid, WarningLevel.ATTENTION.name(), WarningRule.ACTIVITY_DROP.name(), "最近14天活动参与较前14天下降至少一半", JsonValues.toJson(List.of("前期=" + priorActivity, "近期=" + recentActivity)), WarningStatus.OPEN.name());
            }
        }
        audit.record("ANALYZE", "warning_record", 0, "执行规则预警分析");
        return ApiResult.ok(Map.of("source", "TEMPLATE", "message", "已按规则完成趋势筛查", "created", created, "disclaimer", "AI辅助建议，仅供教师参考"));
    }

    private void checkStatus(Map<String,Object> warning,Review review){if(!Objects.equals(warning.get("status"),review.expectedStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"预警状态已变化，请重新读取后处理");}
    private Map<String,Object> owned(long id,HttpServletRequest request,boolean lock){
        var warnings=db.query(SELECT+"where w.id=? and c.teacher_id=?"+(lock?" for update":""),(rs,n)->row(rs),id,actor(request));
        if(warnings.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"预警不存在或不属于当前教师");return warnings.get(0);
    }
    private Map<String,Object> row(ResultSet rs)throws SQLException{
        Map<String,Object> data=new LinkedHashMap<>();
        data.put("id",rs.getLong("id"));data.put("studentId",rs.getLong("student_id"));data.put("studentName",rs.getString("student_name"));data.put("studentNo",rs.getString("student_no"));
        data.put("level",rs.getString("level"));data.put("status",rs.getString("status"));data.put("summary",rs.getString("summary"));data.put("ruleCode",rs.getString("rule_code"));data.put("teacherNote",rs.getString("teacher_note"));
        data.put("createdAt",rs.getTimestamp("created_at").toString());
        try{data.put("evidence",json.readTree(rs.getString("evidence_json")));}catch(Exception e){data.put("evidence",List.of("原始证据格式异常，请核对数据源"));}
        return data;
    }
    private long actor(HttpServletRequest request){Object id=request.getAttribute("userId");if(id==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录");return Long.parseLong(id.toString());}
}
