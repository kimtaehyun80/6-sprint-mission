package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.Map;


public class UserAlreadyExistException extends UserException {

  public UserAlreadyExistException(String duplicatedValue) {
    super(ErrorCode.DUPLICATE_USER, Map.of("duplicated_value", duplicatedValue));
  }
}
