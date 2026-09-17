package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.AiAnalysis;
import org.apache.ibatis.annotations.Mapper;

/** AI 分析记录的数据访问：保留送模型的字段白名单与返回内容，供教师追溯。 */
@Mapper
public interface AnalysisMapper {

    /** 返回自增主键并回填到 {@code analysis.id}。 */
    int insert(AiAnalysis analysis);
}
