package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.StudentPlanExecution;
import com.zhixiaojiang.model.po.StudentPlanFeedback;
import com.zhixiaojiang.model.po.StudentPlanPublication;
import com.zhixiaojiang.model.vo.PortalPlanContext;
import com.zhixiaojiang.model.vo.PortalPlanExecutionRow;
import com.zhixiaojiang.model.vo.PortalPlanFeedbackRow;
import com.zhixiaojiang.model.vo.PortalPlanRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 成长计划（学生可见版本）的数据访问。
 *
 * <p>发布版本不可修改：一次发布一行，学生端只看到最新版本；执行与反馈用请求键去重，
 * 重复请求必须内容一致。
 */
@Mapper
public interface StudentPlanMapper {

    /** 学生可见的最新版本列表（草稿与历史版本不出现）。 */
    List<PortalPlanRow> visibleToStudent(@Param("studentId") long studentId);

    /** 教师查看某方案的全部发布版本。 */
    List<PortalPlanRow> versionsOfPlan(@Param("planId") long planId);

    /** 学生端取版本上下文：仅限本人且方案非草稿。 */
    Optional<PortalPlanContext> contextForStudent(@Param("publicationId") long publicationId, @Param("studentId") long studentId,
                                                 @Param("lock") boolean lock);

    /** 教师端取版本上下文：按班级归属校验。 */
    Optional<PortalPlanContext> contextForTeacher(@Param("publicationId") long publicationId, @Param("teacherId") long teacherId,
                                                 @Param("lock") boolean lock);

    /** 当前已发布的最大版本号；没有版本时为 0。 */
    int latestVersion(@Param("planId") long planId);

    List<PortalPlanExecutionRow> executions(@Param("publicationId") long publicationId);

    List<PortalPlanFeedbackRow> feedback(@Param("publicationId") long publicationId);

    Optional<PortalPlanExecutionRow> executionByRequestKey(@Param("publicationId") long publicationId, @Param("requestKey") String requestKey);

    Optional<PortalPlanFeedbackRow> feedbackByRequestKey(@Param("publicationId") long publicationId, @Param("requestKey") String requestKey);

    /** 返回自增主键并回填到实体 id。 */
    int insertPublication(StudentPlanPublication publication);

    int insertExecution(StudentPlanExecution execution);

    int insertFeedback(StudentPlanFeedback feedback);
}
