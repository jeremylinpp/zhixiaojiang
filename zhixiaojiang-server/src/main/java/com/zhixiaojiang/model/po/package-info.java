/**
 * 表实体（PO）：与数据库表一一对应，仅承载字段，不放业务逻辑。
 *
 * <p>由 {@code scripts/generate-po.py} 从 {@code schema.sql} 生成，表结构变更后重新执行该脚本。
 * 面向接口响应的查询结果放在 {@code model.vo}，避免把表的其余列带到前端。
 */
package com.zhixiaojiang.model.po;
