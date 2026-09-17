package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.TaskSubmission;
import com.zhixiaojiang.model.vo.PortalTaskRow;
import com.zhixiaojiang.model.vo.StudentTaskView;
import com.zhixiaojiang.model.vo.TaskSubmissionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 任务成果提交的数据访问。
 *
 * <p>提交用请求键去重；学生只能操作自己且任务已发布（g.status='PUBLISHED'）的记录。
 */
@Mapper
public interface TaskSubmissionMapper {

    List<PortalTaskRow> tasksOf(@Param("studentId") long studentId);

    /** 学生端归属校验：任务属于自己且已发布；lock=true 时加锁。 */
    Optional<StudentTaskView> owned(@Param("studentTaskId") long studentTaskId, @Param("studentId") long studentId, @Param("lock") boolean lock);

    List<TaskSubmissionRow> ofStudentTask(@Param("studentTaskId") long studentTaskId);

    Optional<TaskSubmissionRow> byRequestKey(@Param("studentTaskId") long studentTaskId, @Param("requestKey") String requestKey);

    /** 最新一次提交，用于教师审核时的版本校验。 */
    Optional<TaskSubmissionRow> latest(@Param("studentTaskId") long studentTaskId);

    Optional<Long> latestId(@Param("studentTaskId") long studentTaskId);

    /** 返回自增主键并回填到实体 id。 */
    int insert(TaskSubmission submission);

    int markSubmitted(@Param("studentTaskId") long studentTaskId);

    int markReturned(@Param("studentTaskId") long studentTaskId, @Param("note") String note);

    int returnSubmission(@Param("submissionId") long submissionId, @Param("feedback") String feedback, @Param("reviewer") long reviewer);

    int completeSubmission(@Param("submissionId") long submissionId, @Param("feedback") String feedback, @Param("reviewer") long reviewer);
}
