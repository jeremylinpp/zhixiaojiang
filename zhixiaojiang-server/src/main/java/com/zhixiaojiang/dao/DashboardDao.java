package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.constant.StudentStatus;
import com.zhixiaojiang.common.constant.WarningStatus;
import com.zhixiaojiang.common.util.RowMaps;
import com.zhixiaojiang.common.util.SqlParams;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 驾驶舱统计的数据访问。
 *
 * <p>出勤率的分母只算已登记记录，分子为出勤与迟到；这里的集合字面量属于查询语义，保留在 SQL 中。
 */
@Repository
public class DashboardDao {
    private final JdbcTemplate db;

    public DashboardDao(JdbcTemplate db) {
        this.db = db;
    }

    public int countActiveStudents(List<Long> classIds) {
        Integer count = db.queryForObject("select count(*) from student where class_id in " + SqlParams.inClause(classIds) + " and status=?", Integer.class, SqlParams.append(classIds.toArray(), StudentStatus.ACTIVE.name()));
        return count == null ? 0 : count;
    }

    public LocalDate latestAttendanceDate(List<Long> classIds) {
        return db.queryForObject("select max(ar.attendance_date) from attendance_record ar join student s on s.id=ar.student_id where s.class_id in " + SqlParams.inClause(classIds) + " and s.status=?", LocalDate.class, SqlParams.append(classIds.toArray(), StudentStatus.ACTIVE.name()));
    }

    public Map<String, Object> attendanceSummary(List<Long> classIds, LocalDate date) {
        return db.queryForObject("select count(*) registered_count, round(100.0*sum(case when ar.status in ('PRESENT','LATE') then 1 else 0 end)/nullif(count(*),0),1) attendance_rate from attendance_record ar join student s on s.id=ar.student_id where s.class_id in " + SqlParams.inClause(classIds) + " and s.status=? and ar.attendance_date=?", RowMaps.mapper(), SqlParams.append(SqlParams.append(classIds.toArray(), StudentStatus.ACTIVE.name()), date));
    }

    public List<Map<String, Object>> targets(List<Long> classIds) {
        return db.query("select name,target_value,current_value,unit,status from class_target where class_id in " + SqlParams.inClause(classIds) + " order by id", RowMaps.mapper(), classIds.toArray());
    }

    public List<Map<String, Object>> openWarnings(List<Long> classIds) {
        return db.query("select w.id,w.level,w.summary,s.name student_name from warning_record w join student s on s.id=w.student_id where s.class_id in " + SqlParams.inClause(classIds) + " and w.status=? order by w.created_at desc limit 50", RowMaps.mapper(), SqlParams.append(classIds.toArray(), WarningStatus.OPEN.name()));
    }
}
