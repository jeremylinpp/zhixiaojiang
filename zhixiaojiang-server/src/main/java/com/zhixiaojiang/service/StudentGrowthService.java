package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.dao.GrowthMapper;
import com.zhixiaojiang.dao.StudentGrowthMapper;
import com.zhixiaojiang.model.po.ActivityRecord;
import com.zhixiaojiang.model.po.StudentGrowthSubmission;
import com.zhixiaojiang.model.vo.DimensionAverages;
import com.zhixiaojiang.model.vo.PortalGrowthRecordRow;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class StudentGrowthService {
    private final StudentGrowthMapper submissions;
    private final StudentScope students;
    private final TeacherScope teachers;
    private final GrowthMapper growth;
    private final AuditRecorder audit;
    private final StudentMessageService messages;

    public StudentGrowthService(StudentGrowthMapper submissions, StudentScope students, TeacherScope teachers, GrowthMapper growth, AuditRecorder audit, StudentMessageService messages) {
        this.submissions = submissions;
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
        data.put("scores", submissions.scores(id, start, end));
        data.put("evaluations", submissions.evaluations(id, start, end));
        data.put("skills", submissions.skills(id, start, end));
        data.put("activities", submissions.activities(id, start, end));
        data.put("records", submissions.recordsBetween(id, start, end));
        return data;
    }

    @Transactional
    public Map<String, Object> submit(Submission body) {
        long student = students.studentId();
        submissions.lockStudent(student);
        var previous = submissions.submissionByRequestKey(student, body.requestKey());
        if (previous.isPresent()) {
            PortalGrowthRecordRow row = previous.get();
            Long linked = submissions.previousIdOf(row.getId()).orElse(null);
            if (!Objects.equals(body.previousId(), linked))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "重复请求关联记录已变化");
            if (!body.category().equals(row.getCategory()) || !body.title().trim().equals(row.getTitle()) || !body.content().trim().equals(row.getContent()) || !body.occurredOn().toString().equals(row.getOccurredOn().toString()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "重复请求内容已变化");
            return Map.of("id", row.getId(), "saved", false);
        }
        if (body.previousId() != null) {
            String status = submissions.submissionStatus(body.previousId(), student)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "原成长记录不存在"));
            if (!"RETURNED".equals(status) || submissions.revisionCount(body.previousId()) != 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "仅能补充尚未重新提交的退回记录");
        }
        StudentGrowthSubmission record = new StudentGrowthSubmission();
        record.setStudentId(student);
        record.setRequestKey(body.requestKey());
        record.setCategory(body.category());
        record.setTitle(body.title().trim());
        record.setContent(body.content().trim());
        record.setOccurredOn(body.occurredOn());
        submissions.insertSubmission(record);
        long id = record.getId();
        if (body.previousId() != null) submissions.insertRevision(body.previousId(), id);
        audit.record("SUBMIT_GROWTH", "student_growth_submission", id, "学生提交成长记录，待教师审核");
        return Map.of("id", id, "saved", true);
    }

    public Map<String, Object> pending(long studentId) {
        teachers.requireStudent(studentId);
        return Map.of("items", submissions.recordsOf(studentId));
    }

    @Transactional
    public Map<String, Object> review(long id, Review review) {
        PortalGrowthRecordRow row = submissions.forReview(id, teachers.teacher())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "成长记录不存在"));
        if (!"PENDING".equals(row.getStatus())) {
            if (review.status().equals(row.getStatus()) && review.feedback().trim().equals(row.getFeedback()))
                return Map.of("saved", false);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "记录已经审核，请刷新查看");
        }
        submissions.review(id, review.status(), review.feedback().trim(), teachers.teacher());
        if ("APPROVED".equals(review.status()) && "ACTIVITY".equals(row.getCategory())) {
            ActivityRecord activity = new ActivityRecord();
            activity.setStudentId(row.getStudentId());
            activity.setActivityDate(row.getOccurredOn());
            activity.setActivityType("学生成长活动");
            activity.setStatus("PARTICIPATED");
            activity.setDetail(row.getTitle());
            activity.setCreatedBy(teachers.teacher());
            submissions.insertActivity(activity);
        }
        audit.record("REVIEW_GROWTH", "student_growth_submission", id, "教师审核学生成长记录：" + review.status());
        messages.send(row.getStudentId(), "growth-review:" + id, "成长记录审核有新结果", "教师已审核你的成长记录，请查看反馈。", "成长");
        return Map.of("saved", true);
    }
}
