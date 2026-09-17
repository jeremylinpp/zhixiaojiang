package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.InterventionPlan;
import com.zhixiaojiang.model.po.InterventionRecord;
import com.zhixiaojiang.model.vo.InterventionPlanDetail;
import com.zhixiaojiang.model.vo.InterventionPlanRow;
import com.zhixiaojiang.model.vo.InterventionRecordRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 一人一策方案与执行过程记录的数据访问。 */
@Mapper
public interface InterventionMapper {

    List<InterventionPlanRow> ofTeacher(@Param("teacherId") long teacherId);

    Optional<InterventionPlanDetail> detail(@Param("planId") long planId);

    List<InterventionRecordRow> records(@Param("planId") long planId);

    boolean warningBelongsToStudent(@Param("warningId") long warningId, @Param("studentId") long studentId);

    /** 返回自增主键并回填到 {@code plan.id}。 */
    int insert(InterventionPlan plan);

    int update(@Param("planId") long planId, @Param("title") String title, @Param("teacherNote") String teacherNote,
               @Param("reviewAt") LocalDate reviewAt);

    int updateStatus(@Param("planId") long planId, @Param("status") String status);

    int updateReview(@Param("planId") long planId, @Param("reviewAt") LocalDate reviewAt, @Param("teacherNote") String teacherNote);

    int insertRecord(InterventionRecord record);

    /** 已确认的方案一旦记录执行过程即进入执行中。 */
    int startIfConfirmed(@Param("planId") long planId, @Param("inProgress") String inProgress, @Param("confirmed") String confirmed);
}
