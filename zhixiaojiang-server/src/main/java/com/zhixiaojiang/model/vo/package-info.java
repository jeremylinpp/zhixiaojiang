/**
 * 查询结果投影（VO）：MyBatis 直接映射到这些类，JSON 响应的字段名即类的属性名。
 *
 * <p>每个 VO 只包含对应接口实际返回的字段，避免把表里其他列带到前端；
 * 表实体放 {@code model.po}，仅用于服务内部读写。
 */
package com.zhixiaojiang.model.vo;
