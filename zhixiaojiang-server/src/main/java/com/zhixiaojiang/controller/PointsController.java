package com.zhixiaojiang.controller;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RequestValues;
import com.zhixiaojiang.common.constant.PointCategory;
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
  private final TeacherScope scope;
  private final AuditRecorder audit;
  public PointsController(JdbcTemplate db, TeacherScope scope, AuditRecorder audit) { this.db = db; this.scope = scope; this.audit = audit; }
  public record Award(@Positive long studentId, Integer amount, Long ruleId,
      @NotBlank @Size(max=160) String reason, @NotBlank @Size(max=100) String idempotencyKey) {}

  @GetMapping("/students/{id}/points")
  Map<String,Object> list(@PathVariable long id, @RequestParam(defaultValue="1") int page,
      @RequestParam(defaultValue="20") int pageSize, HttpServletRequest request) {
    scope.requireStudent(id, request);
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
    return ApiResult.ok(Map.of("items",items,"page",current,"pageSize",size,
        "total",db.queryForObject("select count(*) from point_ledger where student_id=?",Long.class,id),
        "balance",db.queryForObject("select coalesce(sum(amount),0) from point_ledger where student_id=?",Long.class,id)));
  }

  @PostMapping("/points") @Transactional
  Map<String,Object> award(@Valid @RequestBody Award award, HttpServletRequest request) {
    long teacher = scope.requireStudent(award.studentId(),request);
    Integer amount = award.amount();
    PointCategory category = PointCategory.MANUAL;
    if (award.ruleId() != null) {
      var rules = db.queryForList("select amount from point_rule where id=? and enabled=true",award.ruleId());
      if (rules.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"积分规则不存在或已停用");
      amount = ((Number)rules.get(0).get("amount")).intValue(); category = PointCategory.RULE;
    }
    if (amount == null || amount == 0 || amount < -1000 || amount > 1000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"积分必须是 -1000 至 1000 的非零整数");
    String key = "teacher:"+teacher+":"+award.idempotencyKey();
    int changed = db.update("insert ignore into point_ledger(student_id,amount,category,reason,idempotency_key,created_by) values(?,?,?,?,?,?)",award.studentId(),amount,category.name(),award.reason().trim(),key,teacher);
    var saved = db.queryForMap("select id,student_id,amount,category,reason from point_ledger where idempotency_key=?",key);
    if (((Number)saved.get("student_id")).longValue()!=award.studentId() || ((Number)saved.get("amount")).intValue()!=amount || !saved.get("reason").equals(award.reason().trim()) || !saved.get("category").equals(category.name()))
      throw new ResponseStatusException(HttpStatus.CONFLICT,"重复请求的内容发生变化，请重新创建记录");
    long id = ((Number)saved.get("id")).longValue();
    if(changed==1) audit.record(teacher,"CREATE","point_ledger",id,"录入机智币");
    return ApiResult.ok(Map.of("id",id,"saved",changed==1,"idempotencyKey",award.idempotencyKey()));
  }

  @PostMapping("/points/{id}/reverse") @Transactional
  Map<String,Object> reverse(@PathVariable long id, HttpServletRequest request) {
    var matches = db.queryForList("select student_id,amount,category,reason from point_ledger where id=?",id);
    if(matches.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"积分记录不存在");
    var original = matches.get(0);
    long teacher = scope.requireStudent(((Number)original.get("student_id")).longValue(),request);
    if (PointCategory.REVERSAL.name().equals(original.get("category"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"反向流水不能再次撤销，请新建纠错记录");
    String reason = "撤销："+original.get("reason");
    int changed = db.update("insert ignore into point_ledger(student_id,amount,category,reason,idempotency_key,created_by) values(?,?,?,?,?,?)",original.get("student_id"),-((Number)original.get("amount")).intValue(),PointCategory.REVERSAL.name(),reason.substring(0,Math.min(160,reason.length())),"reverse:"+id,teacher);
    if(changed==1) audit.record(teacher,"REVERSE","point_ledger",id,"撤销机智币，保留原流水");
    return ApiResult.ok(Map.of("saved",changed==1));
  }

    /** 积分规则是全校共用的量纲目录，不含学生数据，因此按启用状态全局可读。 */
    @GetMapping("/point-rules")
    Map<String, Object> pointRules() {
        return ApiResult.ok(Map.of("items", db.queryForList("select id,name,category,amount,enabled,description from point_rule where enabled=true order by id")));
    }

    @PostMapping("/point-rules")
    Map<String, Object> createPointRule(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        long id = JdbcInsert.returningId(db, "insert into point_rule(name,category,amount,enabled,description,created_by) values(?,?,?,?,?,?)", RequestValues.text(b, "name", "新积分规则"), RequestValues.text(b, "category", "MANUAL"), RequestValues.intValue(b.get("amount")), b.getOrDefault("enabled", true), b.get("description"), scope.teacher(req));
        audit.record(req, "CREATE", "point_rule", id, "创建积分规则");
        return ApiResult.ok(Map.of("id", id));
    }

}
