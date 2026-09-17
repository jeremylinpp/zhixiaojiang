package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.TaskAttachment;
import com.zhixiaojiang.model.vo.AttachmentDigest;
import com.zhixiaojiang.model.vo.AttachmentMeta;
import com.zhixiaojiang.model.vo.TaskAttachmentRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 任务附件的数据访问。
 *
 * <p>附件二进制放在库里（file_bytes），只在下载时单独取出，列表查询不带这一列。
 */
@Mapper
public interface TaskAttachmentMapper {

    /** 同一任务同一请求键的已有附件，用于重复上传校验（比对摘要与文件名）。 */
    Optional<AttachmentDigest> byRequestKey(@Param("studentTaskId") long studentTaskId, @Param("requestKey") String requestKey);

    Optional<AttachmentMeta> metadata(@Param("id") long id);

    /** 尚未提交的附件（最多 3 个）。 */
    List<TaskAttachmentRow> stagedOf(@Param("studentTaskId") long studentTaskId);

    List<TaskAttachmentRow> filesOf(@Param("submissionId") long submissionId);

    int stagedCount(@Param("studentTaskId") long studentTaskId);

    /** 该学生全部附件占用的字节数。 */
    long usedBytes(@Param("studentId") long studentId);

    /** 返回自增主键并回填到实体 id。 */
    int insert(TaskAttachment attachment);

    /** 只取二进制内容，避免把整行读进内存。 */
    Optional<TaskAttachment> fileOf(@Param("id") long id);

    int attachToSubmission(@Param("submissionId") long submissionId, @Param("id") long id, @Param("studentTaskId") long studentTaskId);

    List<Long> idsOf(@Param("submissionId") long submissionId);

    int deleteStaged(@Param("id") long id);

    /** 学生端任务归属校验；lock=true 时加锁。 */
    Optional<String> ownedTaskStatus(@Param("studentTaskId") long studentTaskId, @Param("studentId") long studentId, @Param("lock") boolean lock);
}
