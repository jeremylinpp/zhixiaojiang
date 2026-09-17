package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.vo.ClassSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 班级数据访问。 */
@Mapper
public interface ClassRoomMapper {

    /** 教师负责的班级及其在籍学生数。 */
    List<ClassSummary> ofTeacher(@Param("teacherId") long teacherId);
}
