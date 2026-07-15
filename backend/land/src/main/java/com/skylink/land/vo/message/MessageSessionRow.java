package com.skylink.land.vo.message;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MessageSessionRow {

    private String sessionType;

    private Long targetId;

    private String targetName;

    private Long lastMessageId;

    private Long lastMessageSenderId;

    private String lastMessageSenderName;

    private Long receiverId;

    private Long groupId;

    private Integer lastMessageType;

    private String lastMessageContent;

    private Integer lastMessageRecalled;

    private LocalDateTime lastTime;
}
