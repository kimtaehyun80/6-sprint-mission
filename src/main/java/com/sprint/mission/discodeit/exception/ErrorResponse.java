package com.sprint.mission.discodeit.exception;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class ErrorResponse {

  private final Instant timestamp;
  private final int status; //HTTP 상태 코드
  private final String code; //커스텀 오류 코드
  private final String message; //사용자에게 보여줄 오류 메시지
  private final String exceptionType; //발생한 예외의 클래스 이름
  private final Map<String, Object> details; //예외 발생 상황에 대한 추가 정보

  public static ErrorResponse from(DiscodeitException e) {
    ErrorCode errorCode = e.getErrorCode();
    return ErrorResponse.builder()
        .timestamp(e.getTimestamp())
        .status(errorCode.getHttpStatus().value())
        .code(errorCode.name())
        .message(e.getMessage())
        .exceptionType(e.getClass().getSimpleName())
        .details(e.getDetails())
        .build();
  }

  public static ErrorResponse from(Exception e, ErrorCode errorCode) {
    return ErrorResponse.builder()
        .timestamp(Instant.now())
        .status(errorCode.getHttpStatus().value())
        .code(errorCode.name())
        .message(errorCode.getMessage())
        .exceptionType(e.getClass().getSimpleName())
        .details(Map.of("error_detail", "서버 내부에서 예상치 못한 오류가 발생했습니다."))
        .build();
  }
}
