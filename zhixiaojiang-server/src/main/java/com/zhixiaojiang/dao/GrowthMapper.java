package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.AttendanceRecord;
import com.zhixiaojiang.model.po.DimensionEvaluation;
import com.zhixiaojiang.model.po.GrowthRecord;
import com.zhixiaojiang.model.po.ScoreRecord;
import com.zhixiaojiang.model.po.SkillRecord;
import com.zhixiaojiang.model.vo.AttendanceRow;
import com.zhixiaojiang.model.vo.DimensionAverages;
import com.zhixiaojiang.model.vo.EvaluationRow;
import com.zhixiaojiang.model.vo.GrowthRecordRow;
import com.zhixiaojiang.model.vo.ScoreRow;
import com.zhixiaojiang.model.vo.SkillRow;
import com.zhixiaojiang.model.vo.StudentBrief;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 成长工作台的数据访问：周期内的成绩、四维评价、成长记录、技能与出勤，以及五类录入。 */
@Mapper
public interface GrowthMapper {

    Optional<StudentBrief> student(@Param("studentId") long studentId);

    /** 周期内四维评价的平均分；缺失维度返回 null。 */
    DimensionAverages dimensionAverages(@Param("studentId") long studentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<ScoreRow> scores(@Param("studentId") long studentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<EvaluationRow> evaluations(@Param("studentId") long studentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<GrowthRecordRow> growthRecords(@Param("studentId") long studentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<AttendanceRow> attendance(@Param("studentId") long studentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<SkillRow> skills(@Param("studentId") long studentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    int countSameExam(@Param("studentId") long studentId, @Param("subject") String subject, @Param("examName") String examName);

    /** 返回自增主键并回填到 {@code record.id}。 */
    int insertExam(ScoreRecord record);

    /** 该学生当天的出勤记录 id（取第一条），用于“同一天重复登记按更新处理”。 */
    Optional<Long> attendanceIdOn(@Param("studentId") long studentId, @Param("date") LocalDate date);

    int insertAttendance(AttendanceRecord record);

    int updateAttendance(@Param("id") long id, @Param("status") String status, @Param("note") String note, @Param("actor") long actor);

    int insertSkill(SkillRecord record);

    int insertGrowth(GrowthRecord record);

    int insertEvaluation(DimensionEvaluation record);
}
