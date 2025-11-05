package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.Map;


public class PrivateChannelUpdateException extends ChannelException {

  public PrivateChannelUpdateException(Long channelId, Long requestingUserId) {
    super(ErrorCode.PRIVATE_CHANNEL_UPDATE,
        Map.of("channel_id", channelId, "requesting_user_id", requestingUserId));
  }
}
