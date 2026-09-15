package com.zhixiaojiang;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@RestController @RequestMapping("/api/v1/warnings")
public class WarningReviewController {
    private final JdbcTemplate db; private final ObjectMapper json;
    public WarningReviewController(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
    public record Review(@NotBlank @Size(max=400) String note,
                         @NotBlank @Pattern(regexp="OPEN|REVIEWED") String expectedStatus,
                         boolean escalate) {}
    private static final String SELECT="select w.*,s.name student_name,s.student_no from warning_record w join student s on s.id=w.student_id join class_room c on c.id=s.class_id ";
    @GetMapping Map<String,Object> list(@RequestParam(defaultValue="OPEN") String status,
        @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int page,
        @RequestParam(defaultValue="20") int pageSize,HttpServletRequest request){
        if(!Set.of("ALL","OPEN","REVIEWED","CLOSED").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"未知预警状态");
        int current=Math.max(1,page),size=Math.max(1,Math.min(100,pageSize));
        String where="where c.teacher_id=? and (?='ALL' or w.status=?) and (s.name like ? or s.student_no like ? or w.summary like ?)";
        String like="%"+q.trim()+"%";long actor=actor(request);
        var items=db.query(SELECT+where+" order by w.id desc limit ? offset ?",(rs,n)->row(rs),actor,status,status,like,like,like,size,(current-1)*size);
        Long total=db.queryForObject("select count(*) from warning_record w join student s on s.id=w.student_id join class_room c on c.id=s.class_id "+where,Long.class,actor,status,status,like,like,like);
        return ok(Map.of("items",items,"total",total,"page",current,"pageSize",size));
    }
    @GetMapping("/{id}") Map<String,Object> detail(@PathVariable long id,HttpServletRequest request){
        var warning=owned(id,request,false);
        var events=db.query("select a.id,a.action,a.summary,a.created_at,u.display_name from audit_log a left join sys_user u on u.id=a.actor_id where a.entity_type='warning_record' and a.entity_id=? and a.action in ('TRIAGE','CLOSE') order by a.id",(rs,n)->Map.of("id",rs.getLong("id"),"action",rs.getString("action"),"note",rs.getString("summary"),"createdAt",rs.getTimestamp("created_at").toString(),"actor",Objects.toString(rs.getString("display_name"),"教师")),id);
        return ok(Map.of("warning",warning,"events",events));
    }
    @PostMapping("/{id}/triage") @Transactional
    Map<String,Object> triage(@PathVariable long id,@Valid @RequestBody Review review,HttpServletRequest request){
        var warning=owned(id,request,true);checkStatus(warning,review);
        if(!"OPEN".equals(warning.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"该预警已研判，不能重复提交");
        db.update("update warning_record set status='REVIEWED',teacher_note=?,level=? where id=?",review.note().trim(),review.escalate()?"MANUAL":warning.get("level"),id);
        audit(actor(request),id,"TRIAGE","教师研判："+review.note().trim());return ok(Map.of("saved",true,"status","REVIEWED"));
    }
    @PostMapping("/{id}/close") @Transactional
    Map<String,Object> close(@PathVariable long id,@Valid @RequestBody Review review,HttpServletRequest request){
        var warning=owned(id,request,true);checkStatus(warning,review);
        db.update("update warning_record set status='CLOSED',closed_at=now() where id=?",id);
        audit(actor(request),id,"CLOSE","关闭原因："+review.note().trim());return ok(Map.of("saved",true,"status","CLOSED"));
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
    private void audit(long actor,long id,String action,String note){db.update("insert into audit_log(actor_id,action,entity_type,entity_id,summary) values(?,?,'warning_record',?,?)",actor,action,id,note);}
    private Map<String,Object> ok(Object data){return Map.of("code","0","message","success","data",data,"requestId",UUID.randomUUID().toString());}
}
