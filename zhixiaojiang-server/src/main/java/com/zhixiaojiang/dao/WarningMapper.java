package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.WarningRecord;
import com.zhixiaojiang.model.vo.WarningEventRow;
import com.zhixiaojiang.model.vo.WarningRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 预警数据访问：列表、详情、研判关闭，以及规则筛查所需的统计查询。
 *
 * <p>筛查用的统计只回答可核实的事实（迟到次数、未完成到期任务数、活动参与量），不下结论。
 */
@Mapper
public interface WarningMapper {
    /** 列表的“全部状态”筛选值，仅用于查询，不属于 WarningStatus 的取值域。 */
    String STATUS_ALL = "ALL";

    List<WarningRow> page(@Param("teacherId") long teacherId, @Param("filter") String filter, @Param("like") String like,
                          @Param("size") int size, @Param("offset") int offset);

    long count(@Param("teacherId") long teacherId, @Param("filter") String filter, @Param("like") String like);

    Optional<WarningRow> findOwned(@Param("warningId") long warningId, @Param("teacherId") long teacherId, @Param("lock") boolean lock);

    List<WarningEventRow> events(@Param("warningId") long warningId);

    int markReviewed(@Param("warningId") long warningId, @Param("level") String level, @Param("note") String note);

    int markClosed(@Param("warningId") long warningId);

    List<Long> activeStudentIdsOf(@Param("teacherId") long teacherId);

    /** 最近四次数学成绩的得分率，用于连续下降与低分规则。 */
    List<Double> recentMathRatios(@Param("studentId") long studentId);

    int lateCountSince(@Param("studentId") long studentId, @Param("since") LocalDate since);

    int overdueTaskCount(@Param("studentId") long studentId, @Param("today") LocalDate today);

    int activityCountBetween(@Param("studentId") long studentId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    int activityCountSince(@Param("studentId") long studentId, @Param("since") LocalDate since);

    /** 同一学生同一规则在 OPEN 状态只保留一条（库内唯一键 + insert ignore），重复筛查不新增。 */
    int insertIfAbsent(WarningRecord record);
}
