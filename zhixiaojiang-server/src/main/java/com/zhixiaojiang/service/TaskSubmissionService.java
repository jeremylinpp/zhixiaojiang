package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RowMaps;
import com.zhixiaojiang.model.dto.TaskSubmissionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class TaskSubmissionService {
    private final JdbcTemplate db;
    private final StudentScope students;
    private final TeacherScope teachers;
    private final AuditRecorder audit;
    private final StudentMessageService messages;
    private final TaskAttachmentService attachments;

    public TaskSubmissionService(JdbcTemplate db, StudentScope students, TeacherScope teachers, AuditRecorder audit, StudentMessageService messages, TaskAttachmentService attachments) {
        this.db = db;
        this.students = students;
        this.teachers = teachers;
        this.audit = audit;
        this.messages = messages;
        this.attachments = attachments;
    }

    public Map<String, Object> list() {
        return Map.of("items", db.query("select st.id,st.status,st.completed_on,st.teacher_note,g.title,g.description,g.module,g.due_on,g.point_reward "
                        + "from student_task st join growth_task g on g.id=st.task_id where st.student_id=? and g.status='PUBLISHED' order by g.due_on,st.id",
                RowMaps.mapper(), students.studentId()));
    }

    public Map<String, Object> history(long id, boolean teacher) {
        if (teacher) teachers.requireStudentTask(id, false);
        else owned(id, false);
        var rows = db.query("select id,content,status,feedback,created_at,reviewed_at from task_submission where student_task_id=? order by id desc", RowMaps.mapper(), id);
        rows.forEach(row -> row.put("attachments", attachments.files(((Number) row.get("id")).longValue())));
        return Map.of("items", rows);
    }

    @Transactional
    public Map<String, Object> submit(long id, TaskSubmissionRequest body) {
        var assignment = owned(id, true);
        var fileIds = body.attachmentIds() == null ? java.util.List.<Long>of() : body.attachmentIds();
        var previous = db.query("select id,content from task_submission where student_task_id=? and request_key=?", RowMaps.mapper(), id, body.requestKey());
        if (!previous.isEmpty()) {
            if (!body.content().trim().equals(previous.get(0).get("content")))
                throw conflict("重复请求的内容不一致，请刷新后重试");
            if (!fileIds.stream().sorted().toList().equals(attachments.ids(((Number) previous.get(0).get("id")).longValue())))
                throw conflict("重复请求的附件不一致");
            return Map.of("id", previous.get(0).get("id"), "saved", false);
        }
        if (!"ASSIGNED".equals(assignment.get("status")) && !"RETURNED".equals(assignment.get("status")))
            throw conflict("任务已经提交或完成，不能重复提交");
        long submission = JdbcInsert.returningId(db, "insert into task_submission(student_task_id,request_key,content) values(?,?,?)", id, body.requestKey(), body.content().trim());
        attachments.attach(id, submission, fileIds);
        db.update("update student_task set status='SUBMITTED' where id=?", id);
        audit.record("SUBMIT", "student_task", id, "学生提交任务成果");
        return Map.of("id", submission, "saved", true);
    }

    @Transactional
    public Map<String, Object> returnForChanges(long id, long submissionId, String feedback) {
        var assignment = teachers.requireStudentTask(id, true);
        if (feedback == null || feedback.isBlank() || feedback.length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写 1–500 字补充要求");
        var current = db.query("select id,status,feedback from task_submission where student_task_id=? order by id desc limit 1", RowMaps.mapper(), id);
        if (current.isEmpty() || ((Number) current.get(0).get("id")).longValue() != submissionId)
            throw conflict("提交版本已变化，请刷新后审核");
        if ("RETURNED".equals(assignment.getStatus()) && feedback.trim().equals(current.get(0).get("feedback")))
            return Map.of("saved", false);
        if (!"SUBMITTED".equals(assignment.getStatus())) throw conflict("只能退回待评价的提交");
        db.update("update task_submission set status='RETURNED',feedback=?,reviewed_at=current_timestamp,reviewed_by=? where id=?", feedback.trim(), teachers.teacher(), submissionId);
        db.update("update student_task set status='RETURNED',teacher_note=? where id=?", feedback.trim(), id);
        audit.record("RETURN", "student_task", id, "教师退回任务，请学生补充");
        messages.send(assignment.getStudentId(), "task-return:" + submissionId, "任务需要补充", "教师已给出补充要求，请查看任务反馈。", "任务");
        return Map.of("saved", true);
    }

    private Map<String, Object> owned(long id, boolean lock) {
        return db.query("select st.id,st.status from student_task st join growth_task g on g.id=st.task_id where st.id=? and st.student_id=? and g.status='PUBLISHED'" + (lock ? " for update" : ""),
                        RowMaps.mapper(), id, students.studentId()).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在"));
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
