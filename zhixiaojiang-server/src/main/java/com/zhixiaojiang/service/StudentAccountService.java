package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.CurrentTeacher;
import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.dao.StudentAccountMapper;
import com.zhixiaojiang.model.po.StudentAccount;
import com.zhixiaojiang.model.po.SysUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class StudentAccountService {
    private final StudentAccountMapper accounts;
    private final TeacherScope teachers;
    private final StudentScope students;
    private final CurrentTeacher current;
    private final PasswordEncoder encoder;
    private final AuditRecorder audit;
    public StudentAccountService(StudentAccountMapper accounts,TeacherScope teachers,StudentScope students,CurrentTeacher current,PasswordEncoder encoder,AuditRecorder audit) {
        this.accounts=accounts;this.teachers=teachers;this.students=students;this.current=current;this.encoder=encoder;this.audit=audit;
    }

    public Map<String,Object> account(long studentId) {
        teachers.requireStudent(studentId);
        var account=accounts.accountOfStudent(studentId);
        return account.isEmpty()?Map.of("exists",false):Map.of("exists",true,"account",account.get());
    }

    @Transactional
    public Map<String,Object> create(long studentId,String username,String password) {
        teachers.requireStudent(studentId,true);
        if(username==null || !username.matches("[A-Za-z0-9][A-Za-z0-9_.-]{3,63}")) throw bad("账号需为 4–64 位字母、数字、下划线、点或短横线");
        validatePassword(password);
        if(!"ACTIVE".equals(accounts.studentStatus(studentId))) throw bad("只能为在籍学生开通账号");
        if(accounts.countAccountsOfStudent(studentId)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"该学生已开通账号");
        if(accounts.countUsersByName(username)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"账号名称已被使用");
        SysUser user=new SysUser();
        user.setUsername(username);user.setPasswordHash(encoder.encode(password));user.setDisplayName(accounts.studentName(studentId));user.setRole("STUDENT");
        accounts.insertUser(user);
        StudentAccount account=new StudentAccount();account.setUserId(user.getId());account.setStudentId(studentId);
        accounts.insertAccount(account);
        long id=user.getId();
        audit.record("CREATE_ACCOUNT","student",studentId,"教师开通学生账号（首次登录须改密）");
        return Map.of("username",username,"mustChangePassword",true);
    }

    @Transactional
    public Map<String,Object> changePassword(String oldPassword,String newPassword) {
        students.studentId(false);
        validatePassword(newPassword);
        long id=current.id();
        String hash=accounts.passwordHashForUpdate(id).orElseThrow(()->bad("当前密码不正确"));
        if(oldPassword==null || oldPassword.getBytes(StandardCharsets.UTF_8).length>72 || !encoder.matches(oldPassword,hash)) throw bad("当前密码不正确");
        if(encoder.matches(newPassword,hash)) throw bad("新密码不能与当前密码相同");
        accounts.updatePassword(id,encoder.encode(newPassword));
        accounts.markPasswordChanged(id);
        audit.record("CHANGE_PASSWORD","sys_user",id,"学生修改密码，所有旧会话失效");
        return Map.of("saved",true,"loginRequired",true);
    }

    private void validatePassword(String password) {
        if(password==null || password.length()<12 || password.length()>64 || password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw bad("密码需为 12–64 个字符，UTF-8 编码不超过 72 字节");
    }
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
