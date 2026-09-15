package com.zhixiaojiang.controller;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.StudentStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** 教师资料：当前账号、显示名称与任教班级；账号与权限由管理员维护。 */
@RestController @RequestMapping("/api/v1/profile")
public class ProfileController {
  private final JdbcTemplate db;
  private final TeacherScope scope;
  private final AuditRecorder audit;
  public ProfileController(JdbcTemplate db, TeacherScope scope, AuditRecorder audit){this.db=db;this.scope=scope;this.audit=audit;}
  public record ProfileEdit(@NotBlank @Size(max=80) String displayName,@NotBlank @Size(max=80) String previousDisplayName){}
  @GetMapping Map<String,Object> profile(HttpServletRequest request){
    long id=scope.teacher(request);
    var users=db.query("select username,display_name,role from sys_user where id=?",(rs,n)->Map.of("username",rs.getString("username"),"displayName",rs.getString("display_name"),"role",rs.getString("role")),id);
    if(users.isEmpty())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"账号已失效");
    var classes=db.query("select c.id,c.name,c.grade,c.is_demo,(select count(*) from student s where s.class_id=c.id and s.status=?) student_count from class_room c where c.teacher_id=? order by c.id",(rs,n)->Map.of("id",rs.getLong("id"),"name",rs.getString("name"),"grade",rs.getString("grade"),"isDemo",rs.getBoolean("is_demo"),"studentCount",rs.getInt("student_count")),StudentStatus.ACTIVE.name(),id);
    return ApiResult.ok(Map.of("teacher",users.get(0),"classes",classes));
  }
  @PutMapping @Transactional Map<String,Object> update(@Valid @RequestBody ProfileEdit edit,HttpServletRequest request){
    long id=scope.teacher(request);
    int changed=db.update("update sys_user set display_name=? where id=? and display_name=?",edit.displayName().trim(),id,edit.previousDisplayName());
    if(changed==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"资料已被更新，请重新读取后修改");
    audit.record(id,"UPDATE","sys_user",id,"修改教师显示名称");
    return ApiResult.ok(Map.of("saved",true));
  }
}
