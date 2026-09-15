package com.zhixiaojiang.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** 全局异常出口：统一响应结构，并把服务端异常写入日志（不再静默吞掉堆栈）。 */
@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
  ResponseEntity<Map<String,Object>> duplicate(org.springframework.dao.DuplicateKeyException e) {
    log.warn("唯一键冲突：{}", e.getMostSpecificCause().getMessage());
    return response(HttpStatus.CONFLICT, "该记录已存在，请检查学号或重复提交");
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String,Object>> status(ResponseStatusException e) {
    return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason() == null ? "请求失败" : e.getReason());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e) {
    String detail = e.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .findFirst().orElse("参数不合法");
    log.warn("参数校验失败：{}", detail);
    return response(HttpStatus.BAD_REQUEST, "参数校验失败");
  }

  /** 请求体不是合法 JSON 或枚举/日期无法解析时返回 400，不能算服务端错误。 */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Map<String,Object>> unreadable(HttpMessageNotReadableException e) {
    log.warn("请求体无法解析：{}", e.getMostSpecificCause().getMessage());
    return response(HttpStatus.BAD_REQUEST, "请求体格式不正确");
  }

  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<Map<String,Object>> database(DataAccessException e) {
    log.error("数据访问异常", e);
    return response(HttpStatus.SERVICE_UNAVAILABLE, "数据服务暂时不可用");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String,Object>> unknown(Exception e) {
    log.error("未预期的服务端异常", e);
    return response(HttpStatus.INTERNAL_SERVER_ERROR, "服务暂时不可用");
  }

  private ResponseEntity<Map<String,Object>> response(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(ApiResult.fail(String.valueOf(status.value()), message));
  }
}
