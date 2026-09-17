package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.CurrentTeacher;
import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class StudentAccountService {
    private final JdbcTemplate db;
    private final TeacherScope teachers;
    private final StudentScope students;
    private final CurrentTeacher current;
    private final PasswordEncoder encoder;
    private final AuditRecorder audit;
    public StudentAccountService(JdbcTemplate db,TeacherScope teachers,StudentScope students,CurrentTeacher current,PasswordEncoder encoder,AuditRecorder audit) {
        this.db=db;this.teachers=teachers;this.students=students;this.current=current;this.encoder=encoder;this.audit=audit;
    }

    public Map<String,Object> account(long studentId) {
        teachers.requireStudent(studentId);
        var rows=db.query("select u.username,a.must_change_password from student_account a join sys_user u on u.id=a.user_id where a.student_id=?",RowMaps.mapper(),studentId);
        return rows.isEmpty()?Map.of("exists",false):Map.of("exists",true,"account",rows.get(0));
    }

    @Transactional
    public Map<String,Object> create(long studentId,String username,String password) {
        teachers.requireStudent(studentId,true);
        if(username==null || !username.matches("[A-Za-z0-9][A-Za-z0-9_.-]{3,63}")) throw bad("账号需为 4–64 位字母、数字、下划线、点或短横线");
        validatePassword(password);
        if(!"ACTIVE".equals(db.queryForObject("select status from student where id=?",String.class,studentId))) throw bad("只能为在籍学生开通账号");
        if(db.queryForObject("select count(*) from student_account where student_id=?",Integer.class,studentId)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"该学生已开通账号");
        if(db.queryForObject("select count(*) from sys_user where username=?",Integer.class,username)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"账号名称已被使用");
        String name=db.queryForObject("select name from student where id=?",String.class,studentId);
        long id=JdbcInsert.returningId(db,"insert into sys_user(username,password_hash,display_name,role) values(?,?,?,'STUDENT')",username,encoder.encode(password),name);
        db.update("insert into student_account(user_id,student_id) values(?,?)",id,studentId);
        audit.record("CREATE_ACCOUNT","student",studentId,"教师开通学生账号（首次登录须改密）");
        return Map.of("username",username,"mustChangePassword",true);
    }

    @Transactional
    public Map<String,Object> changePassword(String oldPassword,String newPassword) {
        students.studentId(false);
        validatePassword(newPassword);
        long id=current.id();
        String hash=db.queryForObject("select password_hash from sys_user where id=? for update",String.class,id);
        if(oldPassword==null || oldPassword.getBytes(StandardCharsets.UTF_8).length>72 || !encoder.matches(oldPassword,hash)) throw bad("当前密码不正确");
        if(encoder.matches(newPassword,hash)) throw bad("新密码不能与当前密码相同");
        db.update("update sys_user set password_hash=? where id=?",encoder.encode(newPassword),id);
        db.update("update student_account set must_change_password=false,session_version=session_version+1 where user_id=?",id);
        audit.record("CHANGE_PASSWORD","sys_user",id,"学生修改密码，所有旧会话失效");
        return Map.of("saved",true,"loginRequired",true);
    }

    private void validatePassword(String password) {
        if(password==null || password.length()<12 || password.length()>64 || password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw bad("密码需为 12–64 个字符，UTF-8 编码不超过 72 字节");
    }
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
