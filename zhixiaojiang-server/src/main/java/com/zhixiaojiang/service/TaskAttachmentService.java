package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.dao.StudentMapper;
import com.zhixiaojiang.dao.TaskAttachmentMapper;
import com.zhixiaojiang.model.po.TaskAttachment;
import com.zhixiaojiang.model.vo.AttachmentDigest;
import com.zhixiaojiang.model.vo.AttachmentMeta;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;

@Service
public class TaskAttachmentService {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private final TaskAttachmentMapper attachments;
    private final StudentMapper studentRows;
    private final StudentScope students;
    private final TeacherScope teachers;
    private final AuditRecorder audit;

    public TaskAttachmentService(TaskAttachmentMapper attachments, StudentMapper studentRows, StudentScope students, TeacherScope teachers, AuditRecorder audit) {
        this.attachments = attachments;
        this.studentRows = studentRows;
        this.students = students;
        this.teachers = teachers;
        this.audit = audit;
    }

    @Transactional
    public Map<String, Object> upload(long taskId, String key, MultipartFile file) throws IOException {
        if (key == null || key.isBlank() || key.length() > 80)
            throw error(HttpStatus.BAD_REQUEST, "上传请求标识不合法");
        if (file.isEmpty() || file.getSize() > MAX_BYTES)
            throw error(HttpStatus.PAYLOAD_TOO_LARGE, "附件不能为空且最大为 2 MiB");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (name.isBlank() || name.length() > 180 || name.contains("/") || name.contains("\\") || name.chars().anyMatch(Character::isISOControl))
            throw error(HttpStatus.BAD_REQUEST, "附件名称不合法");
        byte[] bytes = file.getBytes();
        String type = type(bytes, name);
        String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        long student = students.studentId();
        studentRows.lockById(student);
        String taskStatus = ownedTask(taskId, true);
        var prior = attachments.byRequestKey(taskId, key);
        if (prior.isPresent()) {
            AttachmentDigest row = prior.get();
            if (!hash.equals(row.getSha256()) || !name.equals(row.getOriginalName()))
                throw error(HttpStatus.CONFLICT, "重复上传请求内容不一致");
            return Map.of("id", row.getId(), "saved", false);
        }
        if (!List.of("ASSIGNED", "RETURNED").contains(taskStatus))
            throw error(HttpStatus.CONFLICT, "已提交的成果不可再添加附件");
        if (attachments.stagedCount(taskId) >= 3)
            throw error(HttpStatus.CONFLICT, "每次成果最多 3 个附件，请先移除多余附件");
        if (attachments.usedBytes(student) + bytes.length > 50L * 1024 * 1024)
            throw error(HttpStatus.CONFLICT, "附件总空间已达 50 MiB，请联系教师");
        TaskAttachment record = new TaskAttachment();
        record.setStudentTaskId(taskId);
        record.setRequestKey(key);
        record.setOriginalName(name);
        record.setContentType(type);
        record.setSizeBytes(bytes.length);
        record.setSha256(hash);
        record.setFileBytes(bytes);
        attachments.insert(record);
        long id = record.getId();
        audit.record("UPLOAD_ATTACHMENT", "task_attachment", id, "学生上传任务附件");
        return Map.of("id", id, "saved", true);
    }

    public Map<String, Object> staged(long taskId) {
        ownedTask(taskId, false);
        return Map.of("items", attachments.stagedOf(taskId));
    }

    public Download download(long id, boolean teacher) {
        AttachmentMeta row = metadata(id);
        if (teacher) {
            teachers.requireStudentTask(row.getStudentTaskId(), false);
            if (row.getSubmissionId() == null) throw error(HttpStatus.NOT_FOUND, "附件尚未提交");
        } else ownedTask(row.getStudentTaskId(), false);
        byte[] bytes = attachments.fileOf(id).map(TaskAttachment::getFileBytes).orElse(null);
        if (bytes == null) throw error(HttpStatus.NOT_FOUND, "附件不存在");
        return new Download(row.getOriginalName(), bytes);
    }

    @Transactional
    public Map<String, Object> remove(long id) {
        AttachmentMeta row = metadata(id);
        ownedTask(row.getStudentTaskId(), true);
        if (attachments.deleteStaged(id) != 1)
            throw error(HttpStatus.CONFLICT, "已提交的附件不可移除");
        audit.record("REMOVE_ATTACHMENT", "task_attachment", id, "移除未提交的附件");
        return Map.of("saved", true);
    }

    /**
     * Called while the owning task is locked by the submission transaction.
     */
    public void attach(long taskId, long submissionId, List<Long> ids) {
        if (new HashSet<>(ids).size() != ids.size()) throw error(HttpStatus.BAD_REQUEST, "附件不能重复选择");
        for (Long id : ids)
            if (attachments.attachToSubmission(submissionId, id, taskId) != 1)
                throw error(HttpStatus.CONFLICT, "附件不属于当前任务或已经提交");
    }

    public List<Long> ids(long submissionId) {
        return attachments.idsOf(submissionId);
    }

    public List<com.zhixiaojiang.model.vo.TaskAttachmentRow> files(long submissionId) {
        return attachments.filesOf(submissionId);
    }

    private AttachmentMeta metadata(long id) {
        return attachments.metadata(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "附件不存在"));
    }

    private String ownedTask(long id, boolean lock) {
        return attachments.ownedTaskStatus(id, students.studentId(), lock).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "任务不存在"));
    }

    private String type(byte[] b, String name) {
        String extension = name.toLowerCase(Locale.ROOT);
        if (extension.endsWith(".png") && b.length >= 8 && Arrays.equals(Arrays.copyOf(b, 8), new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10}))
            return "image/png";
        if ((extension.endsWith(".jpg") || extension.endsWith(".jpeg")) && b.length >= 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255)
            return "image/jpeg";
        if (extension.endsWith(".pdf") && b.length >= 5 && new String(b, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
            return "application/pdf";
        throw error(HttpStatus.BAD_REQUEST, "仅支持文件头与扩展名匹配的 PNG、JPEG 或 PDF");
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

    public record Download(String name, byte[] bytes) {
    }
}
