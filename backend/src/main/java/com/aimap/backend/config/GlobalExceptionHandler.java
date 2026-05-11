package com.aimap.backend.config;

import com.aimap.backend.common.ApiResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(err -> err.getField() + " " + err.getDefaultMessage())
        .orElse("参数校验失败");
    return ApiResponse.error(400, msg);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ApiResponse<Void> handleBadRequest(IllegalArgumentException ex) {
    return ApiResponse.error(400, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ApiResponse<Void> handleDefault(Exception ex) {
    return ApiResponse.error(500, ex.getMessage() == null ? "服务异常" : ex.getMessage());
  }
}
