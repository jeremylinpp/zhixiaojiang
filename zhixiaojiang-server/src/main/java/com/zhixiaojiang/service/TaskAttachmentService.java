package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.AuditRecorder;
import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate db;
    private final StudentScope students;
    private final TeacherScope teachers;
    private final AuditRecorder audit;

    public TaskAttachmentService(JdbcTemplate db, StudentScope students, TeacherScope teachers, AuditRecorder audit) {
        this.db = db;
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
        db.queryForObject("select id from student where id=? for update", Long.class, student);
        var task = ownedTask(taskId, true);
        var prior = db.query("select id,sha256,original_name from task_attachment where student_task_id=? and request_key=?", RowMaps.mapper(), taskId, key);
        if (!prior.isEmpty()) {
            if (!hash.equals(prior.get(0).get("sha256")) || !name.equals(prior.get(0).get("originalName")))
                throw error(HttpStatus.CONFLICT, "重复上传请求内容不一致");
            return Map.of("id", prior.get(0).get("id"), "saved", false);
        }
        if (!List.of("ASSIGNED", "RETURNED").contains(task.get("status")))
            throw error(HttpStatus.CONFLICT, "已提交的成果不可再添加附件");
        if (db.queryForObject("select count(*) from task_attachment where student_task_id=? and submission_id is null", Integer.class, taskId) >= 3)
            throw error(HttpStatus.CONFLICT, "每次成果最多 3 个附件，请先移除多余附件");
        long used = db.queryForObject("select coalesce(sum(a.size_bytes),0) from task_attachment a join student_task st on st.id=a.student_task_id where st.student_id=?", Long.class, student);
        if (used + bytes.length > 50L * 1024 * 1024)
            throw error(HttpStatus.CONFLICT, "附件总空间已达 50 MiB，请联系教师");
        long id = JdbcInsert.returningId(db, "insert into task_attachment(student_task_id,request_key,original_name,content_type,size_bytes,sha256,file_bytes) values(?,?,?,?,?,?,?)", taskId, key, name, type, bytes.length, hash, bytes);
        audit.record("UPLOAD_ATTACHMENT", "task_attachment", id, "学生上传任务附件");
        return Map.of("id", id, "saved", true);
    }

    public Map<String, Object> staged(long taskId) {
        ownedTask(taskId, false);
        return Map.of("items", db.query("select id,original_name,size_bytes from task_attachment where student_task_id=? and submission_id is null order by id", RowMaps.mapper(), taskId));
    }

    public Download download(long id, boolean teacher) {
        var row = metadata(id);
        if (teacher) {
            teachers.requireStudentTask(((Number) row.get("studentTaskId")).longValue(), false);
            if (row.get("submissionId") == null) throw error(HttpStatus.NOT_FOUND, "附件尚未提交");
        } else ownedTask(((Number) row.get("studentTaskId")).longValue(), false);
        byte[] bytes = db.queryForObject("select file_bytes from task_attachment where id=?", (rs, n) -> rs.getBytes(1), id);
        if (bytes == null) throw error(HttpStatus.NOT_FOUND, "附件不存在");
        return new Download(row.get("originalName").toString(), bytes);
    }

    @Transactional
    public Map<String, Object> remove(long id) {
        var row = metadata(id);
        ownedTask(((Number) row.get("studentTaskId")).longValue(), true);
        if (db.update("delete from task_attachment where id=? and submission_id is null", id) != 1)
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
            if (db.update("update task_attachment set submission_id=? where id=? and student_task_id=? and submission_id is null", submissionId, id, taskId) != 1)
                throw error(HttpStatus.CONFLICT, "附件不属于当前任务或已经提交");
    }

    public List<Long> ids(long submissionId) {
        return db.queryForList("select id from task_attachment where submission_id=? order by id", Long.class, submissionId);
    }

    public List<Map<String, Object>> files(long submissionId) {
        return db.query("select id,original_name,size_bytes from task_attachment where submission_id=? order by id", RowMaps.mapper(), submissionId);
    }

    private Map<String, Object> metadata(long id) {
        return db.query("select student_task_id,submission_id,original_name from task_attachment where id=?", RowMaps.mapper(), id).stream().findFirst().orElseThrow(() -> error(HttpStatus.NOT_FOUND, "附件不存在"));
    }

    private Map<String, Object> ownedTask(long id, boolean lock) {
        return db.query("select st.status from student_task st join growth_task g on g.id=st.task_id where st.id=? and st.student_id=? and g.status='PUBLISHED'" + (lock ? " for update" : ""), RowMaps.mapper(), id, students.studentId()).stream().findFirst().orElseThrow(() -> error(HttpStatus.NOT_FOUND, "任务不存在"));
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
