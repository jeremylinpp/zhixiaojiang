package com.zhixiaojiang.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixiaojiang.common.constant.StudentStatus;
import com.zhixiaojiang.common.constant.WarningStatus;
import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 预警列表、详情、研判与关闭，以及规则筛查所需的统计查询。 */
@Repository
public class WarningDao {
    /** 列表的“全部状态”筛选值，仅用于查询，不属于 WarningStatus 的取值域。 */
    public static final String STATUS_ALL = "ALL";

    private static final String SELECT = "select w.*,s.name student_name,s.student_no from warning_record w join student s on s.id=w.student_id join class_room c on c.id=s.class_id ";
    private static final String EVENTS = "select a.id,a.action,a.summary,a.created_at,u.display_name from audit_log a left join sys_user u on u.id=a.actor_id where a.entity_type='warning_record' and a.entity_id=? and a.action in ('TRIAGE','CLOSE') order by a.id";

    private final JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();

    public WarningDao(JdbcTemplate db) {
        this.db = db;
    }

    public List<Map<String, Object>> page(long teacherId, String filter, String like, int size, int offset) {
        return db.query(SELECT + whereClause() + " order by w.id desc limit ? offset ?", (rs, n) -> row(rs), teacherId, filter, filter, like, like, like, size, offset);
    }

    public long count(long teacherId, String filter, String like) {
        Long total = db.queryForObject("select count(*) from warning_record w join student s on s.id=w.student_id join class_room c on c.id=s.class_id " + whereClause(), Long.class, teacherId, filter, filter, like, like, like);
        return total == null ? 0 : total;
    }

    public Optional<Map<String, Object>> findOwned(long warningId, long teacherId, boolean lock) {
        return db.query(SELECT + "where w.id=? and c.teacher_id=?" + (lock ? " for update" : ""), (rs, n) -> row(rs), warningId, teacherId).stream().findFirst();
    }

    /** 研判与关闭的过程记录，来自审计表。 */
    public List<Map<String, Object>> events(long warningId) {
        return db.query(EVENTS, (rs, n) -> {
            Map<String, Object> event = new java.util.LinkedHashMap<>();
            event.put("id", rs.getLong("id"));
            event.put("action", rs.getString("action"));
            event.put("note", rs.getString("summary"));
            event.put("createdAt", rs.getTimestamp("created_at").toString());
            event.put("actor", java.util.Objects.toString(rs.getString("display_name"), "教师"));
            return event;
        }, warningId);
    }

    public int markReviewed(long warningId, String level, String note) {
        return db.update("update warning_record set status=?,teacher_note=?,level=? where id=?", WarningStatus.REVIEWED.name(), note, level, warningId);
    }

    public int markClosed(long warningId) {
        return db.update("update warning_record set status=?,closed_at=now() where id=?", WarningStatus.CLOSED.name(), warningId);
    }

    public List<Long> activeStudentIdsOf(long teacherId) {
        return db.queryForList("select s.id from student s join class_room c on c.id=s.class_id where c.teacher_id=? and s.status=?", Long.class, teacherId, StudentStatus.ACTIVE.name());
    }

    /** 最近四次数学成绩的得分率，用于连续下降与低分规则。 */
    public List<Double> recentMathRatios(long studentId) {
        return db.queryForList("select score/full_score ratio from score_record where student_id=? and subject='数学' order by occurred_on desc limit 4", Double.class, studentId);
    }

    public int lateCountSince(long studentId, LocalDate since) {
        Integer count = db.queryForObject("select count(*) from attendance_record where student_id=? and status=? and attendance_date>=?", Integer.class, studentId, com.zhixiaojiang.common.constant.AttendanceStatus.LATE.name(), since);
        return count == null ? 0 : count;
    }

    public int overdueTaskCount(long studentId, LocalDate today) {
        Integer count = db.queryForObject("select count(*) from student_task st join growth_task gt on gt.id=st.task_id where st.student_id=? and gt.due_on<? and st.status<>?", Integer.class, studentId, today, com.zhixiaojiang.common.constant.StudentTaskStatus.COMPLETED.name());
        return count == null ? 0 : count;
    }

    public int activityCountBetween(long studentId, LocalDate from, LocalDate to) {
        Integer count = db.queryForObject("select count(*) from activity_record where student_id=? and activity_date>=? and activity_date<?", Integer.class, studentId, from, to);
        return count == null ? 0 : count;
    }

    public int activityCountSince(long studentId, LocalDate since) {
        Integer count = db.queryForObject("select count(*) from activity_record where student_id=? and activity_date>=?", Integer.class, studentId, since);
        return count == null ? 0 : count;
    }

    /** 同一学生同一规则在 OPEN 状态只保留一条，重复筛查不新增。 */
    public int insertIfAbsent(long studentId, String level, String ruleCode, String summary, String evidenceJson) {
        return db.update("insert ignore into warning_record(student_id,level,rule_code,summary,evidence_json,status) values(?,?,?,?,?,?)", studentId, level, ruleCode, summary, evidenceJson, WarningStatus.OPEN.name());
    }

    private String whereClause() {
        return "where c.teacher_id=? and (?='" + STATUS_ALL + "' or w.status=?) and (s.name like ? or s.student_no like ? or w.summary like ?)";
    }

    /** 预警行：证据由 JSON 列解析，解析失败时给出可核对的提示。 */
    private Map<String, Object> row(ResultSet rs) throws SQLException {
        Map<String, Object> row = RowMaps.of(rs);
        row.remove("closedAt");
        row.put("evidence", evidence(String.valueOf(row.remove("evidenceJson"))));
        return row;
    }

    private List<Object> evidence(String raw) {
        if (raw == null || raw.isBlank() || "null".equals(raw)) return List.of();
        try {
            return json.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {
            });
        } catch (Exception e) {
            return List.of("原始证据格式异常，请核对数据源");
        }
    }
}
