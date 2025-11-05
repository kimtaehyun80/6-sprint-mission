package com.sprint.mission.discodeit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicChannelUpdateRequest(

    @NotBlank(message = "채널 이름은 공백이 될 수 없습니다.")
    @Size(min = 2, max = 50, message = "채널 이름은 2자 이상 50자 이하여야 합니다.")
    String newName,

    @Size(max = 255, message = "채널 설명은 255자 이하여야 합니다.")
    String newDescription
) {
}
