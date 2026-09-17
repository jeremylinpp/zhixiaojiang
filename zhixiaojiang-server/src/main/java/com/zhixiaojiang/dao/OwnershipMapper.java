package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.vo.ClassBrief;
import com.zhixiaojiang.model.vo.PlanOwnership;
import com.zhixiaojiang.model.vo.StudentTaskOwnership;
import com.zhixiaojiang.model.vo.TaskRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 归属校验的数据访问：只回答“这条记录是否属于该教师的班级”，不含业务判断。
 *
 * <p>{@code lock=true} 时会锁定对应行，供状态流转等并发写入使用。
 * 授权边界（越权返回什么状态码）由 {@link com.zhixiaojiang.auth.TeacherScope} 决定。
 */
@Mapper
public interface OwnershipMapper {

    List<Long> classIdsOf(@Param("teacherId") long teacherId);

    /** 命中则返回班级 id（存在即归属）；lock=true 时对命中行加锁，供并发写入使用。 */
    Optional<Long> classOwnedBy(@Param("classId") long classId, @Param("teacherId") long teacherId, @Param("lock") boolean lock);

    /** 命中则返回学生 id（存在即归属）；lock=true 时对命中行加锁。 */
    Optional<Long> studentOwnedBy(@Param("studentId") long studentId, @Param("teacherId") long teacherId, @Param("lock") boolean lock);

    Optional<PlanOwnership> planOwnedBy(@Param("planId") long planId, @Param("teacherId") long teacherId, @Param("lock") boolean lock);

    Optional<Long> targetClassOwnedBy(@Param("targetId") long targetId, @Param("teacherId") long teacherId);

    Optional<TaskRow> taskOwnedBy(@Param("taskId") long taskId, @Param("teacherId") long teacherId, @Param("lock") boolean lock);

    Optional<StudentTaskOwnership> studentTaskOwnedBy(@Param("studentTaskId") long studentTaskId, @Param("teacherId") long teacherId, @Param("lock") boolean lock);

    Optional<ClassBrief> classOf(@Param("classId") long classId);
}
