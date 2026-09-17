package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.StudentMessage;
import com.zhixiaojiang.model.vo.StudentMessageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 学生消息的数据访问。写入在业务事务内执行：不先于业务结果提交。 */
@Mapper
public interface StudentMessageMapper {

    /** 同一学生同一事件只保留一条（库内唯一键 + insert ignore），重复触发不新增。 */
    int insertIfAbsent(StudentMessage message);

    List<StudentMessageRow> page(@Param("studentId") long studentId, @Param("size") int size, @Param("offset") int offset);

    long count(@Param("studentId") long studentId);

    long unreadCount(@Param("studentId") long studentId);

    int countOwned(@Param("id") long id, @Param("studentId") long studentId);

    int markRead(@Param("id") long id, @Param("studentId") long studentId);
}
