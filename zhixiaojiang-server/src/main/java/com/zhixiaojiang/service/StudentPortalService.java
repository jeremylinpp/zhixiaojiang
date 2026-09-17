package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.common.util.RowMaps;
import com.zhixiaojiang.dao.PointMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class StudentPortalService {
    private final JdbcTemplate db;
    private final StudentScope scope;
    private final PointMapper points;

    public StudentPortalService(JdbcTemplate db, StudentScope scope, PointMapper points) {
        this.db = db;
        this.scope = scope;
        this.points = points;
    }

    public Map<String, Object> home() {
        long id = scope.studentId();
        var student = db.queryForObject("select s.name,s.student_no,c.name class_name from student s "
                + "join class_room c on c.id=s.class_id where s.id=?", RowMaps.mapper(), id);
        var tasks = db.query("select st.id,g.title,g.module,g.due_on,st.status from student_task st "
                + "join growth_task g on g.id=st.task_id where st.student_id=? and st.status in ('ASSIGNED','RETURNED') "
                + "and g.status='PUBLISHED' order by g.due_on,st.id", RowMaps.mapper(), id);
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
