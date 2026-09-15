package com.zhixiaojiang;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class PointsController {
  private final JdbcTemplate db;
  public PointsController(JdbcTemplate db) { this.db = db; }
  public record Award(@Positive long studentId, Integer amount, Long ruleId,
      @NotBlank @Size(max=160) String reason, @NotBlank @Size(max=100) String idempotencyKey) {}

  @GetMapping("/students/{id}/points")
  Map<String,Object> list(@PathVariable long id, @RequestParam(defaultValue="1") int page,
      @RequestParam(defaultValue="20") int pageSize, HttpServletRequest request) {
    ownStudent(id, request);
    int size = Math.max(1, Math.min(100, pageSize)), current = Math.max(1,page);
    var items = db.query("select p.id,p.amount,p.category,p.reason,p.created_at,p.idempotency_key,exists(select 1 from point_ledger r where r.idempotency_key=concat('reverse:',p.id)) reversed from point_ledger p where p.student_id=? order by p.id desc limit ? offset ?", (rs,n) -> {
      Map<String,Object> row = new LinkedHashMap<>();
      row.put("id",rs.getLong("id")); row.put("amount",rs.getInt("amount"));
      row.put("category",rs.getString("category")); row.put("reason",rs.getString("reason"));
      row.put("createdAt",rs.getTimestamp("created_at").toLocalDateTime().toString());
      row.put("reversed",rs.getBoolean("reversed"));
      String key = rs.getString("idempotency_key");
      row.put("reversalOf",key.startsWith("reverse:") ? Long.parseLong(key.substring(8)) : null);
      return row;
    },id,size,(current-1)*size);
    return ok(Map.of("items",items,"page",current,"pageSize",size,
        "total",db.queryForObject("select count(*) from point_ledger where student_id=?",Long.class,id),
        "balance",db.queryForObject("select coalesce(sum(amount),0) from point_ledger where student_id=?",Long.class,id)));
  }

  @PostMapping("/points") @Transactional
  Map<String,Object> award(@Valid @RequestBody Award award, HttpServletRequest request) {
    long teacher = ownStudent(award.studentId(),request);
    Integer amount = award.amount();
    String category = "MANUAL";
    if (award.ruleId() != null) {
      var rules = db.queryForList("select amount from point_rule where id=? and enabled=true",award.ruleId());
      if (rules.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"积分规则不存在或已停用");
      amount = ((Number)rules.get(0).get("amount")).intValue(); category = "RULE";
    }
    if (amount == null || amount == 0 || amount < -1000 || amount > 1000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"积分必须是 -1000 至 1000 的非零整数");
    String key = "teacher:"+teacher+":"+award.idempotencyKey();
    int changed = db.update("insert ignore into point_ledger(student_id,amount,category,reason,idempotency_key,created_by) values(?,?,?,?,?,?)",award.studentId(),amount,category,award.reason().trim(),key,teacher);
    var saved = db.queryForMap("select id,student_id,amount,category,reason from point_ledger where idempotency_key=?",key);
    if (((Number)saved.get("student_id")).longValue()!=award.studentId() || ((Number)saved.get("amount")).intValue()!=amount || !saved.get("reason").equals(award.reason().trim()) || !saved.get("category").equals(category))
      throw new ResponseStatusException(HttpStatus.CONFLICT,"重复请求的内容发生变化，请重新创建记录");
    long id = ((Number)saved.get("id")).longValue();
    if(changed==1) audit(teacher,"CREATE",id,"录入机智币");
    return ok(Map.of("id",id,"saved",changed==1,"idempotencyKey",award.idempotencyKey()));
  }

  @PostMapping("/points/{id}/reverse") @Transactional
  Map<String,Object> reverse(@PathVariable long id, HttpServletRequest request) {
    var matches = db.queryForList("select student_id,amount,category,reason from point_ledger where id=?",id);
    if(matches.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"积分记录不存在");
    var original = matches.get(0);
    long teacher = ownStudent(((Number)original.get("student_id")).longValue(),request);
    if ("REVERSAL".equals(original.get("category"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"反向流水不能再次撤销，请新建纠错记录");
    String reason = "撤销："+original.get("reason");
    int changed = db.update("insert ignore into point_ledger(student_id,amount,category,reason,idempotency_key,created_by) values(?,?,'REVERSAL',?,?,?)",original.get("student_id"),-((Number)original.get("amount")).intValue(),reason.substring(0,Math.min(160,reason.length())),"reverse:"+id,teacher);
    if(changed==1) audit(teacher,"REVERSE",id,"撤销机智币，保留原流水");
    return ok(Map.of("saved",changed==1));
  }

  private long ownStudent(long id,HttpServletRequest request) {
    Object actor = request.getAttribute("userId");
    if(actor==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录");
    long teacher = Long.parseLong(actor.toString());
    if(db.queryForObject("select count(*) from student s join class_room c on c.id=s.class_id where s.id=? and c.teacher_id=?",Integer.class,id,teacher)==0)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,"学生不存在或不属于当前教师");
    return teacher;
  }
  private void audit(long actor,String action,long id,String summary) {
    db.update("insert into audit_log(actor_id,action,entity_type,entity_id,summary) values(?,?,'point_ledger',?,?)",actor,action,id,summary);
  }
  private Map<String,Object> ok(Object data) { return Map.of("code","0","message","success","data",data,"requestId",UUID.randomUUID().toString()); }
}
