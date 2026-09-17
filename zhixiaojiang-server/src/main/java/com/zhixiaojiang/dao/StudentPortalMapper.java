package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.vo.PortalHomeStudent;
import com.zhixiaojiang.model.vo.PortalTodoRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/** 学生端首页的数据访问：本人档案摘要与待办任务。 */
@Mapper
public interface StudentPortalMapper {

    Optional<PortalHomeStudent> homeStudent(@Param("studentId") long studentId);

    /** 待办：已指派或已退回，且任务仍在发布状态。 */
    List<PortalTodoRow> todos(@Param("studentId") long studentId);
}
