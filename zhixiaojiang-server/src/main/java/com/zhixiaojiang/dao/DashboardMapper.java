package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.vo.AttendanceSummary;
import com.zhixiaojiang.model.vo.ClassTargetRow;
import com.zhixiaojiang.model.vo.WarningBrief;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 驾驶舱统计的数据访问。
 *
 * <p>出勤率的分母只算已登记记录（nullif 防零除），分子为出勤与迟到；
 * 集合字面量（PRESENT/LATE/ACTIVE）属于查询语义，保留在 SQL 中。
 */
@Mapper
public interface DashboardMapper {

    int countActiveStudents(@Param("classIds") List<Long> classIds);

    LocalDate latestAttendanceDate(@Param("classIds") List<Long> classIds);

    AttendanceSummary attendanceSummary(@Param("classIds") List<Long> classIds, @Param("date") LocalDate date);

    List<ClassTargetRow> targets(@Param("classIds") List<Long> classIds);

    List<WarningBrief> openWarnings(@Param("classIds") List<Long> classIds);
}
