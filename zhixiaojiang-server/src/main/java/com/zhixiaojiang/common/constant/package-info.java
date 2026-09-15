/**
 * 业务取值域常量：状态、等级、维度、模块、类别等。
 *
 * <p>约定：枚举常量名与数据库中的存储值完全一致，写入数据库时统一使用 {@code name()}，
 * 避免在业务代码里散落字符串字面量；解析未知值时返回 {@code null}，由调用方决定如何响应。
 *
 * <p>范围：具有分支判断或多处写入的值使用枚举；仅在单条 SQL 里出现一次的取值
 * （如 {@code growth_task.status='PUBLISHED'}、{@code class_target.status='IN_PROGRESS'}）
 * 暂时保留在 SQL 中，待产品明确取值域后再补枚举。
 */
package com.zhixiaojiang.common.constant;
