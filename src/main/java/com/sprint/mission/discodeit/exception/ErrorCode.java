package com.sprint.mission.discodeit.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // 400 Bad Request
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다."),
  DUPLICATE_USER(HttpStatus.BAD_REQUEST, "이미 존재하는 사용자 정보(이메일 또는 닉네임)입니다."),
  PRIVATE_CHANNEL_UPDATE(HttpStatus.BAD_REQUEST, "비공개 채널의 정보는 수정할 수 없습니다."),

  // 404 Not Found
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
  CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 채널을 찾을 수 없습니다."),
  MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 메시지를 찾을 수 없습니다."),
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 파일을 찾을 수 없습니다."),

  // 401 Unauthorized / 403 Forbidden
  NOT_A_CHANNEL_MEMBER(HttpStatus.FORBIDDEN, "채널 멤버가 아니어서 접근 권한이 없습니다."),
  FORBIDDEN_MESSAGE_OPERATION(HttpStatus.FORBIDDEN, "메시지 수정/삭제 권한이 없습니다."),

  // 500 Internal Server Error
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

  private final HttpStatus httpStatus;
  private final String message;
}
