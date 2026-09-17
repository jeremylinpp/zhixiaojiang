package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.dao.TaskSubmissionMapper;
import com.zhixiaojiang.model.dto.TaskSubmissionRequest;
import com.zhixiaojiang.model.po.TaskSubmission;
import com.zhixiaojiang.model.vo.StudentTaskView;
import com.zhixiaojiang.model.vo.TaskSubmissionRow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class TaskSubmissionService {
    private final TaskSubmissionMapper submissions;
    private final StudentScope students;
    private final TeacherScope teachers;
    private final AuditRecorder audit;
    private final StudentMessageService messages;
    private final TaskAttachmentService attachments;

    public TaskSubmissionService(TaskSubmissionMapper submissions, StudentScope students, TeacherScope teachers, AuditRecorder audit, StudentMessageService messages, TaskAttachmentService attachments) {
        this.submissions = submissions;
        this.students = students;
        this.teachers = teachers;
        this.audit = audit;
        this.messages = messages;
        this.attachments = attachments;
    }

    public Map<String, Object> list() {
        return Map.of("items", submissions.tasksOf(students.studentId()));
    }

    public Map<String, Object> history(long id, boolean teacher) {
        if (teacher) teachers.requireStudentTask(id, false);
        else owned(id, false);
        var rows = submissions.ofStudentTask(id);
        rows.forEach(row -> row.setAttachments(attachments.files(row.getId())));
        return Map.of("items", rows);
    }

    @Transactional
    public Map<String, Object> submit(long id, TaskSubmissionRequest body) {
        var assignment = owned(id, true);
        var fileIds = body.attachmentIds() == null ? java.util.List.<Long>of() : body.attachmentIds();
        var previous = submissions.byRequestKey(id, body.requestKey());
        if (previous.isPresent()) {
            TaskSubmissionRow row = previous.get();
            if (!body.content().trim().equals(row.getContent()))
                throw conflict("重复请求的内容不一致，请刷新后重试");
            if (!fileIds.stream().sorted().toList().equals(attachments.ids(row.getId())))
                throw conflict("重复请求的附件不一致");
            return Map.of("id", row.getId(), "saved", false);
        }
        if (!"ASSIGNED".equals(assignment.getStatus()) && !"RETURNED".equals(assignment.getStatus()))
            throw conflict("任务已经提交或完成，不能重复提交");
        TaskSubmission record = new TaskSubmission();
        record.setStudentTaskId(id);
        record.setRequestKey(body.requestKey());
        record.setContent(body.content().trim());
        submissions.insert(record);
        long submission = record.getId();
        attachments.attach(id, submission, fileIds);
        submissions.markSubmitted(id);
        audit.record("SUBMIT", "student_task", id, "学生提交任务成果");
        return Map.of("id", submission, "saved", true);
    }

    @Transactional
    public Map<String, Object> returnForChanges(long id, long submissionId, String feedback) {
        var assignment = teachers.requireStudentTask(id, true);
        if (feedback == null || feedback.isBlank() || feedback.length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写 1–500 字补充要求");
        var current = submissions.latest(id);
        if (current.isEmpty() || current.get().getId() != submissionId)
            throw conflict("提交版本已变化，请刷新后审核");
        if ("RETURNED".equals(assignment.getStatus()) && feedback.trim().equals(current.get().getFeedback()))
            return Map.of("saved", false);
        if (!"SUBMITTED".equals(assignment.getStatus())) throw conflict("只能退回待评价的提交");
        submissions.returnSubmission(submissionId, feedback.trim(), teachers.teacher());
        submissions.markReturned(id, feedback.trim());
        audit.record("RETURN", "student_task", id, "教师退回任务，请学生补充");
        messages.send(assignment.getStudentId(), "task-return:" + submissionId, "任务需要补充", "教师已给出补充要求，请查看任务反馈。", "任务");
        return Map.of("saved", true);
    }

    private StudentTaskView owned(long id, boolean lock) {
        return submissions.owned(id, students.studentId(), lock)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在"));
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
