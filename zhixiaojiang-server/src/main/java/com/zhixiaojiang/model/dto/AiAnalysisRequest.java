package com.zhixiaojiang.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * AI 辅助分析请求。
 *
 * <p>只提交学生 id：送模型的数据由服务端按白名单字段查询组装，客户端提交的其他字段一律忽略，
 * 避免分析内容被伪造，也避免身份信息混入。
 */
public record AiAnalysisRequest(@NotNull @Positive Long studentId) {
}
