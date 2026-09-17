package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/** 审计留痕的数据访问：只追加，不修改。 */
@Mapper
public interface AuditMapper {

    int insert(AuditLog log);
}
