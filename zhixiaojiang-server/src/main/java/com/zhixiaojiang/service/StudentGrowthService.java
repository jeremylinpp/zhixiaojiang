package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RowMaps;
import com.zhixiaojiang.dao.GrowthMapper;
import com.zhixiaojiang.model.vo.DimensionAverages;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class StudentGrowthService {
    private final JdbcTemplate db;
    private final StudentScope students;
    private final TeacherScope teachers;
    private final GrowthMapper growth;
    private final AuditRecorder audit;
    private final StudentMessageService messages;

    public StudentGrowthService(JdbcTemplate db, StudentScope students, TeacherScope teachers, GrowthMapper growth, AuditRecorder audit, StudentMessageService messages) {
        this.db = db;
        this.students = students;
        this.teachers = teachers;
        this.growth = growth;
        this.audit = audit;
        this.messages = messages;
    }

    public record Submission(@NotBlank @Size(max = 80) String requestKey,
                             @NotBlank @Pattern(regexp = "ACTIVITY|SKILL|REFLECTION") String category,
                             @NotBlank @Size(max = 160) String title, @NotBlank @Size(max = 2000) String content,
                             @NotNull @PastOrPresent LocalDate occurredOn, @Positive Long previousId) {
    }

    public record Review(@NotBlank @Pattern(regexp = "APPROVED|RETURNED") String status,
                         @NotBlank @Size(max = 500) String feedback) {
    }

    public Map<String, Object> workspace(LocalDate from, LocalDate to) {
        long id = students.studentId();
        LocalDate end = to == null ? LocalDate.now() : to, start = from == null ? end.withDayOfYear(1) : from;
        if (start.isAfter(end)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "开始日期不能晚于结束日期");
        Map<String, Object> data = new LinkedHashMap<>();
        // 无评价数据时聚合行全为 null（MyBatis 映射为 null），按四个维度都缺失处理
        DimensionAverages dimensions = java.util.Optional.ofNullable(growth.dimensionAverages(id, start, end))
                .orElseGet(DimensionAverages::new);
        data.put("dimensions", dimensions);
        List<BigDecimal> scores = java.util.Arrays.asList(dimensions.getMoral(), dimensions.getSkill(), dimensions.getThinking(), dimensions.getSmart());
        boolean complete = scores.stream().allMatch(java.util.Objects::nonNull);
        data.put("growthIndex", complete ? scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(4), 1, RoundingMode.HALF_UP) : null);
        // Explicit field allowlists: no internal behavior, warning or conversation notes.
        data.put("scores", db.query("select id,subject,exam_name,score,full_score,occurred_on from score_record where student_id=? and occurred_on between ? and ? order by occurred_on,id", RowMaps.mapper(), id, start, end));
        data.put("evaluations", db.query("select period_start,period_end,moral_score,skill_score,thinking_score,smart_score,evidence from dimension_evaluation where student_id=? and period_end between ? and ? order by period_end desc", RowMaps.mapper(), id, start, end));
        data.put("skills", db.query("select id,skill_name,score,level,occurred_on,evidence from skill_record where student_id=? and occurred_on between ? and ? order by occurred_on desc,id desc", RowMaps.mapper(), id, start, end));
        data.put("activities", db.query("select id,activity_date,activity_type,detail from activity_record where student_id=? and activity_date between ? and ? order by activity_date desc,id desc", RowMaps.mapper(), id, start, end));
        data.put("records", db.query("select r.id,r.category,r.title,r.content,r.occurred_on,r.status,r.feedback,old.previous_id,new.replacement_id as replaced_by from student_growth_submission r left join student_growth_revision old on old.replacement_id=r.id left join student_growth_revision new on new.previous_id=r.id where r.student_id=? and r.occurred_on between ? and ? order by r.occurred_on desc,r.id desc", RowMaps.mapper(), id, start, end));
        return data;
    }

    @Transactional
    public Map<String, Object> submit(Submission body) {
        long student = students.studentId();
        db.queryForObject("select id from student where id=? for update", Long.class, student);
        var previous = db.query("select id,category,title,content,occurred_on from student_growth_submission where student_id=? and request_key=?", RowMaps.mapper(), student, body.requestKey());
        if (!previous.isEmpty()) {
            var row = previous.get(0);
            var links = db.queryForList("select previous_id from student_growth_revision where replacement_id=?", Long.class, row.get("id"));
            if (!Objects.equals(body.previousId(), links.isEmpty() ? null : links.get(0)))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "重复请求关联记录已变化");
            if (!body.category().equals(row.get("category")) || !body.title().trim().equals(row.get("title")) || !body.content().trim().equals(row.get("content")) || !body.occurredOn().toString().equals(row.get("occurredOn").toString()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "重复请求内容已变化");
            return Map.of("id", row.get("id"), "saved", false);
        }
        if (body.previousId() != null) {
            var original = db.queryForList("select status from student_growth_submission where id=? and student_id=?", String.class, body.previousId(), student);
            if (original.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "原成长记录不存在");
            if (!"RETURNED".equals(original.get(0)) || db.queryForObject("select count(*) from student_growth_revision where previous_id=?", Integer.class, body.previousId()) != 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "仅能补充尚未重新提交的退回记录");
        }
        long id = JdbcInsert.returningId(db, "insert into student_growth_submission(student_id,request_key,category,title,content,occurred_on) values(?,?,?,?,?,?)", student, body.requestKey(), body.category(), body.title().trim(), body.content().trim(), body.occurredOn());
        if (body.previousId() != null) db.update("insert into student_growth_revision(previous_id,replacement_id) values(?,?)", body.previousId(), id);
        audit.record("SUBMIT_GROWTH", "student_growth_submission", id, "学生提交成长记录，待教师审核");
        return Map.of("id", id, "saved", true);
    }

    public Map<String, Object> pending(long studentId) {
        teachers.requireStudent(studentId);
        return Map.of("items", db.query("select r.id,r.category,r.title,r.content,r.occurred_on,r.status,r.feedback,prior.previous_id,next_revision.replacement_id as replaced_by from student_growth_submission r left join student_growth_revision prior on prior.replacement_id=r.id left join student_growth_revision next_revision on next_revision.previous_id=r.id where r.student_id=? order by r.id desc", RowMaps.mapper(), studentId));
    }

    @Transactional
    public Map<String, Object> review(long id, Review review) {
        var rows = db.query("select r.* from student_growth_submission r join student s on s.id=r.student_id join class_room c on c.id=s.class_id where r.id=? and c.teacher_id=? for update", RowMaps.mapper(), id, teachers.teacher());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "成长记录不存在");
        var row = rows.get(0);
        if (!"PENDING".equals(row.get("status"))) {
            if (review.status().equals(row.get("status")) && review.feedback().trim().equals(row.get("feedback")))
                return Map.of("saved", false);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "记录已经审核，请刷新查看");
        }
        db.update("update student_growth_submission set status=?,feedback=?,reviewed_at=current_timestamp,reviewed_by=? where id=?", review.status(), review.feedback().trim(), teachers.teacher(), id);
        if ("APPROVED".equals(review.status()) && "ACTIVITY".equals(row.get("category")))
            db.update("insert into activity_record(student_id,activity_date,activity_type,status,detail,created_by) values(?,?,'学生成长活动','PARTICIPATED',?,?)", row.get("studentId"), row.get("occurredOn"), row.get("title"), teachers.teacher());
        audit.record("REVIEW_GROWTH", "student_growth_submission", id, "教师审核学生成长记录：" + review.status());
        messages.send(((Number) row.get("studentId")).longValue(), "growth-review:" + id, "成长记录审核有新结果", "教师已审核你的成长记录，请查看反馈。", "成长");
        return Map.of("saved", true);
    }
}
