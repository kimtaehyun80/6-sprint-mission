package com.sprint.mission.discodeit.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class DiscodeitExceptionHandler {

  @ExceptionHandler(DiscodeitException.class)
  protected ResponseEntity<ErrorResponse> handleDiscodeitException(DiscodeitException e) {
    ErrorCode errorCode = e.getErrorCode();
    ErrorResponse response = ErrorResponse.from(e);
    return new ResponseEntity<>(response, errorCode.getHttpStatus());
  }

  //유효성 검사 실패 예외 처리
  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    Map<String, String> validationErrors = new HashMap<>();
    e.getBindingResult().getFieldErrors().forEach(error -> {
      validationErrors.put(error.getField(), error.getDefaultMessage());
    });
    final ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    final ErrorResponse response = ErrorResponse.builder()
        .timestamp(Instant.now())
        .status(errorCode.getHttpStatus().value())
        .code(errorCode.name())
        .message(errorCode.getMessage())
        .exceptionType(e.getClass().getSimpleName())
        .details(Map.of("validation_errors", validationErrors))
        .build();
    return new ResponseEntity<>(response, errorCode.getHttpStatus());
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ErrorResponse> handleException(Exception e) {
    final ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    final ErrorResponse response = ErrorResponse.from(e, errorCode);
    return new ResponseEntity<>(response, errorCode.getHttpStatus());
  }
}