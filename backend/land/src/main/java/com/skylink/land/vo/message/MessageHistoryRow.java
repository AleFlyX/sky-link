package com.skylink.land.vo.message;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MessageHistoryRow {

    private Long messageId;

    private Long senderId;

    private String senderName;

    private Long receiverId;

    private Long groupId;

    private Integer messageType;

    private String content;

    private Integer isRecalled;

    private LocalDateTime sendTime;
}
