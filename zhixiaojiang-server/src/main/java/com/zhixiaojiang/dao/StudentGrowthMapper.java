package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.ActivityRecord;
import com.zhixiaojiang.model.po.StudentGrowthSubmission;
import com.zhixiaojiang.model.vo.PortalActivityRow;
import com.zhixiaojiang.model.vo.PortalEvaluationRow;
import com.zhixiaojiang.model.vo.PortalGrowthRecordRow;
import com.zhixiaojiang.model.vo.PortalScoreRow;
import com.zhixiaojiang.model.vo.PortalSkillRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 学生自主提交的成长记录与相关档案数据访问。
 *
 * <p>学生端查询只取对外允许的字段（不含教师内部备注等），投影类即字段白名单。
 * 提交用请求键去重；补充提交通过 student_growth_revision 记录原记录与新记录的关系。
 */
@Mapper
public interface StudentGrowthMapper {

    /** 锁定学生行，保证同一学生提交串行化。 */
    Optional<Long> lockStudent(@Param("studentId") long studentId);

    List<PortalScoreRow> scores(@Param("studentId") long studentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<PortalEvaluationRow> evaluations(@Param("studentId") long studentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<PortalSkillRow> skills(@Param("studentId") long studentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<PortalActivityRow> activities(@Param("studentId") long studentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 周期内的成长记录，带原记录/被补充关系。 */
    List<PortalGrowthRecordRow> recordsBetween(@Param("studentId") long studentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 教师查看某学生全部成长记录。 */
    List<PortalGrowthRecordRow> recordsOf(@Param("studentId") long studentId);

    Optional<PortalGrowthRecordRow> submissionByRequestKey(@Param("studentId") long studentId, @Param("requestKey") String requestKey);

    /** 该记录是否是某条记录的补充版本，是则返回原记录 id。 */
    Optional<Long> previousIdOf(@Param("replacementId") long replacementId);

    Optional<String> submissionStatus(@Param("id") long id, @Param("studentId") long studentId);

    int revisionCount(@Param("previousId") long previousId);

    /** 返回自增主键并回填到实体 id。 */
    int insertSubmission(StudentGrowthSubmission submission);

    int insertRevision(@Param("previousId") long previousId, @Param("replacementId") long replacementId);

    /** 教师待审核取记录：按班级归属校验并加锁。 */
    Optional<PortalGrowthRecordRow> forReview(@Param("id") long id, @Param("teacherId") long teacherId);

    int review(@Param("id") long id, @Param("status") String status, @Param("feedback") String feedback, @Param("reviewer") long reviewer);

    int insertActivity(ActivityRecord activity);
}
