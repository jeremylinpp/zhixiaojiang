package com.zhixiaojiang.dao;

import com.zhixiaojiang.model.po.StudentAccount;
import com.zhixiaojiang.model.po.SysUser;
import com.zhixiaojiang.model.vo.StudentAccountRow;
import com.zhixiaojiang.model.vo.StudentIdentity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 学生账号的数据访问：开通、改密、会话校验。
 *
 * <p>账号与档案一一对应（库内唯一键），这里只回答事实，授权判断留在
 * {@link com.zhixiaojiang.auth.StudentScope} 与业务服务里。
 */
@Mapper
public interface StudentAccountMapper {

    /** 学生账号开通情况；未开通时返回空。 */
    Optional<StudentAccountRow> accountOfStudent(@Param("studentId") long studentId);

    String studentStatus(@Param("studentId") long studentId);

    String studentName(@Param("studentId") long studentId);

    int countAccountsOfStudent(@Param("studentId") long studentId);

    int countUsersByName(@Param("username") String username);

    /** 返回自增主键并回填到 {@code user.id}。 */
    int insertUser(SysUser user);

    int insertAccount(StudentAccount account);

    /** 锁定并取出口令哈希，供改密校验使用。 */
    Optional<String> passwordHashForUpdate(@Param("userId") long userId);

    int updatePassword(@Param("userId") long userId, @Param("passwordHash") String passwordHash);

    /** 改密后把待改密标记置否，并递增会话版本使所有旧会话失效。 */
    int markPasswordChanged(@Param("userId") long userId);

    /** 登录时取学生的待改密标记与会话版本。 */
    Optional<StudentIdentity> identityOfUser(@Param("userId") long userId);

    /** 学生账号对应的在籍学生 id，用于把请求映射到学生本人。 */
    Optional<Long> activeStudentIdOfUser(@Param("userId") long userId);

    Optional<Boolean> mustChangePassword(@Param("userId") long userId);

    /** 会话版本一致且账号仍有效时返回账号对应的学生 id。 */
    Optional<Long> sessionStudentId(@Param("userId") long userId, @Param("sessionVersion") long sessionVersion);
}
