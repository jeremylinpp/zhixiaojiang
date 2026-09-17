package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.GrowthTask;
import com.zhixiaojiang.model.vo.TaskReward;
import com.zhixiaojiang.model.vo.TaskRow;
import com.zhixiaojiang.model.vo.TaskStudentRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 六机成长任务与指派记录的数据访问。
 *
 * <p>任务没有班级字段，按发布者隔离：{@code growth_task.created_by} 即归属。
 * 新任务状态固定为 PUBLISHED（取值域尚未定义，暂不建枚举）。
 */
@Mapper
public interface TaskMapper {

    List<TaskRow> ofTeacher(@Param("teacherId") long teacherId);

    /** 返回自增主键并回填到 {@code task.id}。 */
    int insert(GrowthTask task);

    /** 同一任务同一学生只指派一次（库内唯一键 + insert ignore），返回新增行数。 */
    int assign(@Param("taskId") long taskId, @Param("studentId") long studentId);

    List<TaskStudentRow> studentsOfTask(@Param("taskId") long taskId, @Param("teacherId") long teacherId);

    int complete(@Param("studentTaskId") long studentTaskId, @Param("note") String note);

    /** 完成任务所需的学生与奖励信息。 */
    Optional<TaskReward> rewardOf(@Param("studentTaskId") long studentTaskId);

    /** 任务奖励发币：幂等键固定为 task:记录号，重复确认不会重复发币。 */
    int awardPoints(@Param("studentId") long studentId, @Param("amount") Object amount, @Param("reason") String reason,
                    @Param("idempotencyKey") String idempotencyKey, @Param("actor") long actor);

    int evaluate(@Param("studentTaskId") long studentTaskId, @Param("note") String note);

    Optional<TaskRow> findTask(@Param("taskId") long taskId);
}
