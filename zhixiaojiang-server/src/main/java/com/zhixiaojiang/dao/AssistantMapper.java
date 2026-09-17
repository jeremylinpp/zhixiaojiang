package com.zhixiaojiang.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 助手页所需的工作量统计：已录入的数据量与最近一次规则筛查时间。
 *
 * <p>按表计数拆成三个具名方法而不是传表名：表名不能参数化，拆开后 SQL 里没有动态拼接。
 */
@Mapper
public interface AssistantMapper {

    int countStudents(@Param("classIds") List<Long> classIds);

    int countGrowthRecords(@Param("classIds") List<Long> classIds);

    int countScores(@Param("classIds") List<Long> classIds);

    int countSkills(@Param("classIds") List<Long> classIds);

    /** 最近一次规则筛查时间，取自审计记录；字符串形式保持与旧接口相同的可读格式。 */
    String lastRuleAnalysisAt(@Param("teacherId") long teacherId);
}
