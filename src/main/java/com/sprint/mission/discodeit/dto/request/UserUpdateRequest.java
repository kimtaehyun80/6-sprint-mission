package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

    @NotBlank(message = "사용자 이름은 필수 입력 항목입니다.")
    @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하여야 합니다.")
    String newUsername,

    @NotBlank(message = "email 입력은 필수 항목입니다.")
    @Email(message = "유효한 email 형식이 아닙니다.")
    String newEmail,

    @NotBlank(message = "비밀번호 는 필수 입력 항목입니다.")
    @Size(min = 4, message = "비밀번호는 최소 4자 이상 이어야 합니다.")
    String newPassword
) {
}
