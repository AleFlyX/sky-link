package com.skylink.land.controller;

import com.skylink.land.auth.AuthContext;
import com.skylink.land.dto.common.ApiResponse;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.message.MessageDto;
import com.skylink.land.service.chat.MessageService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ApiResponse<MessageDto.MessageResponse> sendMessage(@RequestBody MessageDto.SendMessageRequest request) {
        return ApiResponse.success("message sent", messageService.sendMessage(AuthContext.requireUserId(), request));
    }

    @GetMapping("/sessions")
    public List<MessageDto.MessageSessionResponse> listSessions() {
        return messageService.listSessions(AuthContext.requireUserId());
    }

    @GetMapping
    public PageResponse<MessageDto.MessageResponse> listMessages(MessageDto.MessageHistoryQueryRequest request) {
        return messageService.listMessages(AuthContext.requireUserId(), request);
    }

    @DeleteMapping("/{messageId}")
    public ApiResponse<MessageDto.MessageResponse> recallMessage(@PathVariable Long messageId) {
        return ApiResponse.success("message recalled", messageService.recallMessage(AuthContext.requireUserId(), messageId));
    }
}
