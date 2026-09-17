package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.dao.PointMapper;
import com.zhixiaojiang.dao.StudentPortalMapper;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class StudentPortalService {
    private final StudentPortalMapper portal;
    private final StudentScope scope;
    private final PointMapper points;

    public StudentPortalService(StudentPortalMapper portal, StudentScope scope, PointMapper points) {
        this.portal = portal;
        this.scope = scope;
        this.points = points;
    }

    public Map<String, Object> home() {
        long id = scope.studentId();
        var student = portal.homeStudent(id).orElseThrow();
        var tasks = portal.todos(id);
        return Map.of("student", student, "todos", tasks, "balance", points.balance(id));
    }

    public Map<String, Object> points(int page, int pageSize) {
        long id = scope.studentId();
        int size = Math.max(1, Math.min(100, pageSize));
        int currentPage = Math.max(1, Math.min(100000, page));
        return Map.of("items", points.page(id, size, (currentPage - 1) * size),
                "total", points.count(id), "balance", points.balance(id), "page", currentPage, "pageSize", size);
    }
}
