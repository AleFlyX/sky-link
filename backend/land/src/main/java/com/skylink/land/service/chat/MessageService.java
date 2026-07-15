package com.skylink.land.service.chat;

import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.message.MessageDto;
import java.util.List;

public interface MessageService {

    MessageDto.MessageResponse sendMessage(Long currentUserId, MessageDto.SendMessageRequest request);

    List<MessageDto.MessageSessionResponse> listSessions(Long currentUserId);

    PageResponse<MessageDto.MessageResponse> listMessages(Long currentUserId, MessageDto.MessageHistoryQueryRequest request);

    MessageDto.MessageResponse recallMessage(Long currentUserId, Long messageId);
}
