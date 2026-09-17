package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.Student;
import com.zhixiaojiang.model.vo.AttendanceRow;
import com.zhixiaojiang.model.vo.BehaviorRow;
import com.zhixiaojiang.model.vo.DimensionScore;
import com.zhixiaojiang.model.vo.EvaluationRow;
import com.zhixiaojiang.model.vo.ScoreRow;
import com.zhixiaojiang.model.vo.SkillRow;
import com.zhixiaojiang.model.vo.StudentListRow;
import com.zhixiaojiang.model.po.BehaviorRecord;
import com.zhixiaojiang.model.vo.StudentSummary;
import com.zhixiaojiang.model.vo.TimelineRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学生档案与成长明细的数据访问。
 *
 * <p>动态条件（班级集合、关键字）在 XML 里用 {@code <foreach>}/{@code <if>} 表达，
 * 不再拼接 SQL 字符串；写入用 {@code useGeneratedKeys} 回填主键。
 */
@Mapper
public interface StudentMapper {

    /** 在籍学生分页；成长指数取成长记录平均值。 */
    List<StudentListRow> page(@Param("classIds") List<Long> classIds,
                              @Param("like") String like,
                              @Param("size") int size,
                              @Param("offset") int offset);

    int countActive(@Param("classIds") List<Long> classIds, @Param("like") String like);

    Student findById(@Param("id") Long id);

    StudentSummary findSummary(@Param("id") Long id);

    /** 四维平均分（按成长记录）。 */
    List<DimensionScore> growthByDimension(@Param("studentId") Long studentId);

    List<ScoreRow> scores(@Param("studentId") Long studentId);

    List<TimelineRow> timeline(@Param("studentId") Long studentId, @Param("withAuthor") boolean withAuthor);

    List<AttendanceRow> attendance(@Param("studentId") Long studentId);

    List<BehaviorRow> behavior(@Param("studentId") Long studentId);

    List<SkillRow> skills(@Param("studentId") Long studentId);

    List<EvaluationRow> evaluations(@Param("studentId") Long studentId);

    int insertBehavior(BehaviorRecord record);

    /** 新增学生；主键回填到入参对象的 id。 */
    int insert(Student student);

    int update(@Param("id") Long id,
               @Param("name") String name,
               @Param("gender") String gender,
               @Param("studentNo") String studentNo);

    int archive(@Param("id") Long id, @Param("status") String status);
}
