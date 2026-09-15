package com.zhixiaojiang;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
  ResponseEntity<Map<String,Object>> duplicate(org.springframework.dao.DuplicateKeyException e) {
    return response(HttpStatus.CONFLICT, "该记录已存在，请检查学号或重复提交");
  }
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String,Object>> status(ResponseStatusException e) {
    return response(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason() == null ? "请求失败" : e.getReason());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e) {
    return response(HttpStatus.BAD_REQUEST, "参数校验失败");
  }

  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<Map<String,Object>> database(DataAccessException e) {
    return response(HttpStatus.SERVICE_UNAVAILABLE, "数据服务暂时不可用");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String,Object>> unknown(Exception e) {
    return response(HttpStatus.INTERNAL_SERVER_ERROR, "服务暂时不可用");
  }

  private ResponseEntity<Map<String,Object>> response(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Map.of(
      "code", String.valueOf(status.value()), "message", message, "data", Map.of(), "requestId", UUID.randomUUID().toString()));
  }
}
