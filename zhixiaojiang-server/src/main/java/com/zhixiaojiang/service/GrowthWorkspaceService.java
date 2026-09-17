package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.constant.AttendanceStatus;
import com.zhixiaojiang.dao.GrowthMapper;
import com.zhixiaojiang.model.po.AttendanceRecord;
import com.zhixiaojiang.model.po.DimensionEvaluation;
import com.zhixiaojiang.model.po.GrowthRecord;
import com.zhixiaojiang.model.po.ScoreRecord;
import com.zhixiaojiang.model.po.SkillRecord;
import com.zhixiaojiang.model.vo.DimensionAverages;
import com.zhixiaojiang.model.dto.AttendanceRequest;
import com.zhixiaojiang.model.dto.EvaluationRequest;
import com.zhixiaojiang.model.dto.ExamRequest;
import com.zhixiaojiang.model.dto.GrowthRequest;
import com.zhixiaojiang.model.dto.SkillRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 成长工作台：周期内的四维评价、成绩、技能、成长记录与出勤汇总，以及五类录入。
 *
 * <p>成长指数只在该周期四个维度全部有评价时才计算，缺失维度返回 null，不用 0 代替。
 */
@Service
public class GrowthWorkspaceService {
    private final GrowthMapper growth;
    private final TeacherScope scope;
    private final AuditRecorder audit;

    public GrowthWorkspaceService(GrowthMapper growth, TeacherScope scope, AuditRecorder audit) {
        this.growth = growth;
        this.scope = scope;
        this.audit = audit;
    }

    public Map<String, Object> workspace(long studentId, LocalDate from, LocalDate to) {
        scope.requireStudent(studentId);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfYear(1) : from;
        if (start.isAfter(end)) throw bad("开始日期不能晚于结束日期");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("student", growth.student(studentId));
        data.put("from", start.toString());
        data.put("to", end.toString());
        // 聚合查询在无评价数据时返回全 null 行，MyBatis 会映射为 null，这里按“四个维度都缺失”处理
        DimensionAverages dimensions = java.util.Optional.ofNullable(growth.dimensionAverages(studentId, start, end))
                .orElseGet(DimensionAverages::new);
        Map<String, Object> averages = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO;
        boolean complete = true;
        // 用 LinkedHashMap 而不是 Map.of：缺失维度为 null，Map.of 不接受 null 值
        Map<String, BigDecimal> raw = new LinkedHashMap<>();
        raw.put("moral", dimensions.getMoral());
        raw.put("skill", dimensions.getSkill());
        raw.put("thinking", dimensions.getThinking());
        raw.put("smart", dimensions.getSmart());
        for (Map.Entry<String, BigDecimal> dimension : raw.entrySet()) {
            BigDecimal value = dimension.getValue();
            if (value == null) {
                complete = false;
                averages.put(dimension.getKey(), null);
            } else {
                sum = sum.add(value);
                averages.put(dimension.getKey(), value.setScale(1, RoundingMode.HALF_UP));
            }
        }
        data.put("dimensions", averages);
        data.put("growthIndex", complete ? sum.divide(BigDecimal.valueOf(4), 1, RoundingMode.HALF_UP) : null);
        data.put("scores", growth.scores(studentId, start, end));
        data.put("evaluations", growth.evaluations(studentId, start, end));
        data.put("growth", growth.growthRecords(studentId, start, end));
        data.put("attendance", growth.attendance(studentId, start, end));
        data.put("skills", growth.skills(studentId, start, end));
        return data;
    }

    @Transactional
    public Map<String, Object> recordExam(long studentId, ExamRequest exam) {
        long actor = scope.requireStudent(studentId, true);
        if (exam.score().compareTo(exam.fullScore()) > 0) throw bad("成绩不能超过满分");
        if (growth.countSameExam(studentId, exam.subject().trim(), exam.examName().trim()) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该学生已有同科目、同考试批次成绩，请勿重复录入");
        ScoreRecord score = new ScoreRecord();
        score.setStudentId(studentId);
        score.setSubject(exam.subject().trim());
        score.setExamName(exam.examName().trim());
        score.setScore(exam.score());
        score.setFullScore(exam.fullScore());
        score.setOccurredOn(exam.occurredOn());
        score.setCreatedBy(actor);
        growth.insertExam(score);
        long id = score.getId();
        audit.record(actor, "CREATE", "score_record", id, "教师录入考试成绩");
        return Map.of("id", id);
    }

    /** 同一天重复登记按更新处理，不产生两条出勤记录。 */
    @Transactional
    public Map<String, Object> recordAttendance(long studentId, AttendanceRequest attendance) {
        long actor = scope.requireStudent(studentId, true);
        var existing = growth.attendanceIdOn(studentId, attendance.attendanceDate());
        long id;
        if (existing.isEmpty()) {
            AttendanceRecord record = new AttendanceRecord();
            record.setStudentId(studentId);
            record.setAttendanceDate(attendance.attendanceDate());
            record.setStatus(attendance.status());
            record.setNote(attendance.note());
            record.setCreatedBy(actor);
            growth.insertAttendance(record);
            id = record.getId();
        } else {
            id = existing.orElseThrow();
            growth.updateAttendance(id, attendance.status(), attendance.note(), actor);
        }
        audit.record(actor, "CREATE", "attendance_record", id, "教师登记出勤：" + attendance.status());
        return Map.of("saved", true, "id", id);
    }

    @Transactional
    public Map<String, Object> recordSkill(long studentId, SkillRequest skill) {
        long actor = scope.requireStudent(studentId, true);
        SkillRecord record = new SkillRecord();
        record.setStudentId(studentId);
        record.setSkillName(skill.skillName().trim());
        record.setScore(skill.score());
        record.setLevel(skill.level());
        record.setOccurredOn(skill.occurredOn());
        record.setEvidence(skill.evidence().trim());
        record.setCreatedBy(actor);
        growth.insertSkill(record);
        long id = record.getId();
        audit.record(actor, "CREATE", "skill_record", id, "教师录入技能记录");
        return Map.of("id", id);
    }

    @Transactional
    public Map<String, Object> recordGrowth(long studentId, GrowthRequest record) {
        long actor = scope.requireStudent(studentId, true);
        GrowthRecord row = new GrowthRecord();
        row.setStudentId(studentId);
        row.setDimension(record.dimension());
        row.setScore(record.score());
        row.setTitle(record.title().trim());
        row.setDetail(record.detail());
        row.setOccurredOn(record.occurredOn());
        row.setSource(record.source().trim());
        row.setCreatedBy(actor);
        growth.insertGrowth(row);
        long id = row.getId();
        audit.record(actor, "CREATE", "growth_record", id, "教师录入成长记录");
        return Map.of("id", id);
    }

    @Transactional
    public Map<String, Object> recordEvaluation(long studentId, EvaluationRequest evaluation) {
        long actor = scope.requireStudent(studentId, true);
        if (evaluation.periodStart().isAfter(evaluation.periodEnd())) throw bad("评价周期开始日期不能晚于结束日期");
        if (evaluation.moralScore() == null && evaluation.skillScore() == null && evaluation.thinkingScore() == null && evaluation.smartScore() == null)
            throw bad("请至少填写一个维度的评价分数");
        DimensionEvaluation row = new DimensionEvaluation();
        row.setStudentId(studentId);
        row.setPeriodStart(evaluation.periodStart());
        row.setPeriodEnd(evaluation.periodEnd());
        row.setMoralScore(evaluation.moralScore());
        row.setSkillScore(evaluation.skillScore());
        row.setThinkingScore(evaluation.thinkingScore());
        row.setSmartScore(evaluation.smartScore());
        row.setEvidence(evaluation.evidence().trim());
        row.setCreatedBy(actor);
        growth.insertEvaluation(row);
        long id = row.getId();
        audit.record(actor, "CREATE", "dimension_evaluation", id, "教师录入四维评价");
        return Map.of("id", id);
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
