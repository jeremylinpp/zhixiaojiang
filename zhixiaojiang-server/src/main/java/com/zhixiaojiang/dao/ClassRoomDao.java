package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.constant.StudentStatus;
import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 班级数据访问。 */
@Repository
public class ClassRoomDao {
    private final JdbcTemplate db;

    public ClassRoomDao(JdbcTemplate db) {
        this.db = db;
    }

    /** 教师负责的班级及其在籍学生数。 */
    public List<Map<String, Object>> ofTeacher(long teacherId) {
        return db.query("select c.id,c.name,c.grade,c.is_demo,(select count(*) from student s where s.class_id=c.id and s.status=?) student_count from class_room c where c.teacher_id=? order by c.id", RowMaps.mapper(), StudentStatus.ACTIVE.name(), teacherId);
    }
}
