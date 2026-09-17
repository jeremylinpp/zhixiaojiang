package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.ClassDiagnosisRecord;
import com.zhixiaojiang.model.po.ClassTarget;
import com.zhixiaojiang.model.vo.ClassTargetRow;
import com.zhixiaojiang.model.vo.DiagnosisRecordRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 班级诊改目标与改进措施记录的数据访问。
 *
 * <p>新建目标状态固定为 IN_PROGRESS；取值域尚未定义，暂不建枚举。
 */
@Mapper
public interface DiagnosisMapper {

    /** 班级范围由调用方（教师授权）给出，SQL 用 foreach 展开，不再拼接 in 子句。 */
    List<ClassTargetRow> targets(@Param("classIds") List<Long> classIds);

    List<DiagnosisRecordRow> records(@Param("targetId") long targetId);

    /** 返回自增主键并回填到 {@code record.id}。 */
    int insertRecord(ClassDiagnosisRecord record);

    /** 返回自增主键并回填到 {@code target.id}。 */
    int insertTarget(ClassTarget target);

    int update(@Param("targetId") long targetId, @Param("targetValue") BigDecimal targetValue,
               @Param("currentValue") BigDecimal currentValue, @Param("status") String status);

    int review(@Param("targetId") long targetId, @Param("currentValue") BigDecimal currentValue, @Param("status") String status);
}
