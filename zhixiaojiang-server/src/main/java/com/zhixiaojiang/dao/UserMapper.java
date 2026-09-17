package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.vo.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/** 教师账号数据访问。 */
@Mapper
public interface UserMapper {

    /** 含口令哈希，仅供登录校验使用；UserAccount 上标了 @JsonIgnore，不会返回给前端。 */
    Optional<UserAccount> findByUsername(@Param("username") String username);

    Optional<UserAccount> findById(@Param("id") long id);

    /** 按旧值校验后更新显示名称，返回受影响行数（0 表示已被他人修改）。 */
    int updateDisplayName(@Param("id") long id, @Param("displayName") String displayName,
                          @Param("previousDisplayName") String previousDisplayName);
}
