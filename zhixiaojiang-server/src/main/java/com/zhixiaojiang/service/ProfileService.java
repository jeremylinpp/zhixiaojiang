package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.dao.ClassRoomDao;
import com.zhixiaojiang.dao.UserDao;
import com.zhixiaojiang.model.dto.ProfileEditRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** 教师资料与任教班级。 */
@Service
public class ProfileService {
    private final UserDao users;
    private final ClassRoomDao classes;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public ProfileService(UserDao users, ClassRoomDao classes, TeacherScope scope, AuditRecorder audit) {
        this.users = users;
        this.classes = classes;
        this.scope = scope;
        this.audit = audit;
    }

    public Map<String, Object> load() {
        long teacherId = scope.teacher();
        var teacher = users.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号已失效"));
        return Map.of("teacher", teacher, "classes", classes.ofTeacher(teacherId));
    }

    @Transactional
    public void update(ProfileEditRequest request) {
        long teacherId = scope.teacher();
        int changed = users.updateDisplayName(teacherId, request.displayName().trim(), request.previousDisplayName());
        if (changed == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "资料已被更新，请重新读取后修改");
        audit.record(teacherId, "UPDATE", "sys_user", teacherId, "修改教师显示名称");
    }
}
