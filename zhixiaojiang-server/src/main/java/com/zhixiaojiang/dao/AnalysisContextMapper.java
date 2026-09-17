package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.vo.AiAttendanceCount;
import com.zhixiaojiang.model.vo.AiBehaviorRow;
import com.zhixiaojiang.model.vo.AiScoreRow;
import com.zhixiaojiang.model.vo.AiSkillRow;
import com.zhixiaojiang.model.vo.AiTaskRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 组装送模型的分析上下文。
 *
 * <p>只取可核实的事实类字段（成绩、出勤、行为、技能、任务），不含姓名、学号、联系方式等身份信息；
 * 由服务端按学生 id 查询，不接受前端提交的分析内容。返回类型即白名单：新增列不会自动进入提示词。
 */
@Mapper
public interface AnalysisContextMapper {

    List<AiScoreRow> scores(@Param("studentId") long studentId);

    /** 最近一段时间的出勤次数分布，例如各状态各多少次。 */
    List<AiAttendanceCount> attendance(@Param("studentId") long studentId, @Param("since") LocalDate since);

    List<AiBehaviorRow> behavior(@Param("studentId") long studentId);

    List<AiSkillRow> skills(@Param("studentId") long studentId);

    List<AiTaskRow> tasks(@Param("studentId") long studentId);
}
