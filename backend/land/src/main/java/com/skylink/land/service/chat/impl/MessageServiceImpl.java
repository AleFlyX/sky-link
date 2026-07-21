package com.skylink.land.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.message.MessageDto;
import com.skylink.land.entity.chat.ChatGroup;
import com.skylink.land.entity.chat.GroupMember;
import com.skylink.land.entity.chat.Message;
import com.skylink.land.entity.identity.User;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.chat.ChatGroupMapper;
import com.skylink.land.mapper.chat.FriendshipMapper;
import com.skylink.land.mapper.chat.GroupMemberMapper;
import com.skylink.land.mapper.chat.MessageMapper;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.service.chat.MessageService;
import com.skylink.land.websocket.MessagePushService;
import com.skylink.land.vo.group.GroupMemberRow;
import com.skylink.land.vo.message.MessageHistoryRow;
import com.skylink.land.vo.message.MessageSessionRow;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class MessageServiceImpl implements MessageService {

    private static final int USER_STATUS_ENABLED = 1;

    private static final int GROUP_STATUS_NORMAL = 1;

    private static final int GROUP_ROLE_OWNER = 1;

    private static final int GROUP_ROLE_ADMIN = 2;

    private static final int GROUP_ROLE_MEMBER = 3;

    private static final int MESSAGE_TYPE_TEXT = 1;

    private static final int MESSAGE_TYPE_EMOJI = 4;

    private static final int MESSAGE_RECALLED = 1;

    private static final int MESSAGE_NOT_RECALLED = 0;

    private static final long RECALL_WINDOW_SECONDS = 120;

    private static final String SESSION_TYPE_SINGLE = "single";

    private static final String SESSION_TYPE_GROUP = "group";

    private static final String RECALLED_CONTENT = "消息已撤回";

    private final MessageMapper messageMapper;

    private final FriendshipMapper friendshipMapper;

    private final ChatGroupMapper chatGroupMapper;

    private final GroupMemberMapper groupMemberMapper;

    private final UserMapper userMapper;

    private final MessagePushService messagePushService;

    public MessageServiceImpl(
        MessageMapper messageMapper,
        FriendshipMapper friendshipMapper,
        ChatGroupMapper chatGroupMapper,
        GroupMemberMapper groupMemberMapper,
        UserMapper userMapper,
        MessagePushService messagePushService
    ) {
        this.messageMapper = messageMapper;
        this.friendshipMapper = friendshipMapper;
        this.chatGroupMapper = chatGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.userMapper = userMapper;
        this.messagePushService = messagePushService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageDto.MessageResponse sendMessage(Long currentUserId, MessageDto.SendMessageRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        // 一条消息只能是单聊或群聊，不能同时指定 receiverId 和 groupId。
        requireExactlyOneTarget(request.getReceiverId(), request.getGroupId());

        // 推送前先确认发送者账号仍启用；被禁用用户不能借既有 WebSocket 或 HTTP 会话继续发消息。
        User sender = requireEnabledUser(currentUserId, "sender user not found");
        String content = normalizeContent(request.getContent());
        int messageType = toMessageTypeCode(request.getMessageType());

        if (request.getReceiverId() != null) {
            return sendSingleMessage(currentUserId, sender, request.getReceiverId(), messageType, content);
        }
        return sendGroupMessage(currentUserId, sender, request.getGroupId(), messageType, content);
    }

    @Override
    public List<MessageDto.MessageSessionResponse> listSessions(Long currentUserId) {
        List<MessageDto.MessageSessionResponse> sessions = new ArrayList<>();
        sessions.addAll(messageMapper.selectSingleSessions(currentUserId).stream()
            .map(this::toSessionResponse)
            .toList());
        sessions.addAll(messageMapper.selectGroupSessions(currentUserId).stream()
            .map(this::toSessionResponse)
            .toList());
        sessions.sort(Comparator.comparing(
            MessageDto.MessageSessionResponse::getLastTime,
            Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed());
        return sessions;
    }

    @Override
    public PageResponse<MessageDto.MessageResponse> listMessages(
        Long currentUserId,
        MessageDto.MessageHistoryQueryRequest request
    ) {
        MessageDto.MessageHistoryQueryRequest query = request == null
            ? new MessageDto.MessageHistoryQueryRequest()
            : request;
        requireExactlyOneTarget(query.getReceiverId(), query.getGroupId());

        int size = query.sizeOrDefault();
        int page = query.getBefore() == null ? query.pageOrDefault() : 1;
        long offset = query.getBefore() == null ? (long) (page - 1) * size : 0L;

        long total;
        List<MessageHistoryRow> rows;
        if (query.getReceiverId() != null) {
            // 单聊双方必须已经是好友，不能通过猜测用户 ID 读取或发送陌生人消息。
            requireSingleChatAccess(currentUserId, query.getReceiverId());
            total = messageMapper.countSingleMessages(currentUserId, query.getReceiverId(), query.getBefore());
            rows = total == 0
                ? List.of()
                : messageMapper.selectSingleMessages(currentUserId, query.getReceiverId(), query.getBefore(), offset, size);
        } else {
            // 群聊记录同样要求当前人仍是这个群的有效成员。
            requireGroupChatAccess(currentUserId, query.getGroupId());
            total = messageMapper.countGroupMessages(currentUserId, query.getGroupId(), query.getBefore());
            rows = total == 0
                ? List.of()
                : messageMapper.selectGroupMessages(currentUserId, query.getGroupId(), query.getBefore(), offset, size);
        }

        if (total == 0) {
            return PageResponse.<MessageDto.MessageResponse>builder()
                .total(0L)
                .page(page)
                .size(size)
                .records(List.of())
                .build();
        }

        List<MessageDto.MessageResponse> records = rows.stream()
            .map(this::toMessageResponse)
            .sorted(Comparator.comparing(MessageDto.MessageResponse::getSendTime))
            .toList();
        return PageResponse.<MessageDto.MessageResponse>builder()
            .total(total)
            .page(page)
            .size(size)
            .records(records)
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageDto.MessageResponse recallMessage(Long currentUserId, Long messageId) {
        if (messageId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "messageId is required");
        }

        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "message not found");
        }
        if (!currentUserId.equals(message.getSenderId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only the sender can recall this message");
        }
        if (Integer.valueOf(MESSAGE_RECALLED).equals(message.getIsRecalled())) {
            throw new BusinessException(ErrorCode.CONFLICT, "message has already been recalled");
        }
        if (message.getSendTime() == null
            || ChronoUnit.SECONDS.between(message.getSendTime(), LocalDateTime.now()) > RECALL_WINDOW_SECONDS) {
            throw new BusinessException(ErrorCode.CONFLICT, "message can only be recalled within 2 minutes");
        }

        int updated = messageMapper.update(
            null,
            new LambdaUpdateWrapper<Message>()
                .eq(Message::getMessageId, messageId)
                .eq(Message::getSenderId, currentUserId)
                .eq(Message::getIsRecalled, MESSAGE_NOT_RECALLED)
                .set(Message::getIsRecalled, MESSAGE_RECALLED)
        );
        if (updated != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "message has already been recalled");
        }

        message.setIsRecalled(MESSAGE_RECALLED);
        MessageDto.MessageResponse response = toMessageResponse(message, displayName(userMapper.selectById(message.getSenderId())));
        // 撤回也要实时通知；否则其他在线页面会继续显示未撤回的旧内容。
        publishRecallEventAfterCommit(message, response);
        return response;
    }

    private MessageDto.MessageResponse sendSingleMessage(
        Long currentUserId,
        User sender,
        Long receiverId,
        int messageType,
        String content
    ) {
        if (currentUserId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cannot send a message to yourself");
        }
        User receiver = requireEnabledUser(receiverId, "receiver user not found");
        requireSingleChatAccess(currentUserId, receiverId);

        Message message = createMessage(currentUserId, receiverId, null, messageType, content);
        MessageDto.MessageResponse response = toMessageResponse(message, displayName(sender));

        MessageDto.MessageSessionResponse senderSession = buildSessionResponse(
            SESSION_TYPE_SINGLE,
            receiverId,
            displayName(receiver),
            response
        );
        MessageDto.MessageSessionResponse receiverSession = buildSessionResponse(
            SESSION_TYPE_SINGLE,
            currentUserId,
            displayName(sender),
            response
        );
        runAfterCommit(() -> {
            // 发送者自己的其他标签页也要收到事件，才能同步会话预览；不是只推给接收者。
            messagePushService.push("message.created", response, senderSession, List.of(currentUserId));
            // 接收者可能同时开多个页面，注册表会把这一条事件复制给其所有在线 session。
            messagePushService.push("message.created", response, receiverSession, List.of(receiverId));
        });
        return response;
    }

    private MessageDto.MessageResponse sendGroupMessage(
        Long currentUserId,
        User sender,
        Long groupId,
        int messageType,
        String content
    ) {
        ChatGroup group = requireActiveGroup(groupId);
        requireGroupChatAccess(currentUserId, groupId);

        Message message = createMessage(currentUserId, null, groupId, messageType, content);
        MessageDto.MessageResponse response = toMessageResponse(message, displayName(sender));
        MessageDto.MessageSessionResponse session = buildSessionResponse(
            SESSION_TYPE_GROUP,
            groupId,
            group.getGroupName(),
            response
        );
        // 群消息只推给当前有效群成员；具体成员列表由后端查询，前端不能自行指定接收者。
        List<Long> recipients = activeGroupMemberIds(groupId, currentUserId);
        runAfterCommit(() -> messagePushService.push("message.created", response, session, recipients));
        return response;
    }

    private Message createMessage(Long senderId, Long receiverId, Long groupId, int messageType, String content) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setGroupId(groupId);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setSendTime(LocalDateTime.now());
        message.setIsRecalled(MESSAGE_NOT_RECALLED);
        messageMapper.insert(message);
        return message;
    }

    private void publishRecallEventAfterCommit(Message message, MessageDto.MessageResponse response) {
        if (message.getReceiverId() != null) {
            User sender = userMapper.selectById(message.getSenderId());
            User receiver = userMapper.selectById(message.getReceiverId());
            MessageDto.MessageSessionResponse senderSession = buildSessionResponse(
                SESSION_TYPE_SINGLE,
                message.getReceiverId(),
                displayNameOrFallback(receiver, message.getReceiverId()),
                response
            );
            MessageDto.MessageSessionResponse receiverSession = buildSessionResponse(
                SESSION_TYPE_SINGLE,
                message.getSenderId(),
                displayNameOrFallback(sender, message.getSenderId()),
                response
            );
            runAfterCommit(() -> {
                // 单聊双方各自看到的会话标题不同，因此分别构造并推送对应 session 视图。
                messagePushService.push("message.recalled", response, senderSession, List.of(message.getSenderId()));
                messagePushService.push("message.recalled", response, receiverSession, List.of(message.getReceiverId()));
            });
            return;
        }

        ChatGroup group = chatGroupMapper.selectById(message.getGroupId());
        MessageDto.MessageSessionResponse session = buildSessionResponse(
            SESSION_TYPE_GROUP,
            message.getGroupId(),
            group == null ? "群聊#" + message.getGroupId() : group.getGroupName(),
            response
        );
        // 群撤回通知给所有当前有效成员；发送者也会通过其在线连接收到同步事件。
        List<Long> recipients = activeGroupMemberIds(message.getGroupId(), message.getSenderId());
        runAfterCommit(() -> messagePushService.push("message.recalled", response, session, recipients));
    }

    private void requireExactlyOneTarget(Long receiverId, Long groupId) {
        boolean hasReceiver = receiverId != null;
        boolean hasGroup = groupId != null;
        if (hasReceiver == hasGroup) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "receiverId and groupId must be specified exclusively");
        }
    }

    private void requireSingleChatAccess(Long currentUserId, Long receiverId) {
        if (receiverId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "receiverId is required");
        }
        if (currentUserId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cannot open a single chat with yourself");
        }
        FriendPair pair = FriendPair.of(currentUserId, receiverId);
        if (friendshipMapper.selectByUsers(pair.userId(), pair.friendUserId()) == null) {
            // 后端复查好友关系；前端联系人列表的显示不能当作授权依据。
            throw new BusinessException(ErrorCode.FORBIDDEN, "you can only send messages to friends");
        }
    }

    private void requireGroupChatAccess(Long currentUserId, Long groupId) {
        requireActiveGroup(groupId);
        GroupMember membership = groupMemberMapper.selectMembership(groupId, currentUserId);
        if (membership == null || !isActiveGroupRole(membership.getMemberRole())) {
            // 已退出的成员角色不再拥有读取或发送该群消息的资格。
            throw new BusinessException(ErrorCode.FORBIDDEN, "you are not a member of this group");
        }
    }

    private ChatGroup requireActiveGroup(Long groupId) {
        if (groupId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "groupId is required");
        }
        ChatGroup group = chatGroupMapper.selectOne(
            new LambdaQueryWrapper<ChatGroup>()
                .eq(ChatGroup::getGroupId, groupId)
                .eq(ChatGroup::getStatus, GROUP_STATUS_NORMAL)
                .last("limit 1")
        );
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "group not found");
        }
        return group;
    }

    private User requireEnabledUser(Long userId, String message) {
        User user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(USER_STATUS_ENABLED).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, message);
        }
        return user;
    }

    private boolean isActiveGroupRole(Integer role) {
        return Integer.valueOf(GROUP_ROLE_OWNER).equals(role)
            || Integer.valueOf(GROUP_ROLE_ADMIN).equals(role)
            || Integer.valueOf(GROUP_ROLE_MEMBER).equals(role);
    }

    private List<Long> activeGroupMemberIds(Long groupId, Long fallbackUserId) {
        Set<Long> userIds = new LinkedHashSet<>();
        groupMemberMapper.selectAllActiveMembers(groupId).stream()
            .map(GroupMemberRow::getUserId)
            .forEach(userIds::add);
        if (fallbackUserId != null) {
            userIds.add(fallbackUserId);
        }
        return List.copyOf(userIds);
    }

    private MessageDto.MessageSessionResponse toSessionResponse(MessageSessionRow row) {
        MessageDto.MessageResponse lastMessage = MessageDto.MessageResponse.builder()
            .messageId(row.getLastMessageId())
            .senderId(row.getLastMessageSenderId())
            .senderName(row.getLastMessageSenderName())
            .receiverId(row.getReceiverId())
            .groupId(row.getGroupId())
            .messageType(toMessageTypeName(row.getLastMessageType()))
            .content(displayContent(row.getLastMessageContent(), row.getLastMessageRecalled()))
            .recalled(isRecalled(row.getLastMessageRecalled()))
            .sendTime(row.getLastTime())
            .build();
        return MessageDto.MessageSessionResponse.builder()
            .sessionType(row.getSessionType())
            .targetId(row.getTargetId())
            .targetName(row.getTargetName())
            .lastMessage(lastMessage)
            .lastTime(row.getLastTime())
            .build();
    }

    private MessageDto.MessageResponse toMessageResponse(MessageHistoryRow row) {
        return MessageDto.MessageResponse.builder()
            .messageId(row.getMessageId())
            .senderId(row.getSenderId())
            .senderName(row.getSenderName())
            .receiverId(row.getReceiverId())
            .groupId(row.getGroupId())
            .messageType(toMessageTypeName(row.getMessageType()))
            .content(displayContent(row.getContent(), row.getIsRecalled()))
            .recalled(isRecalled(row.getIsRecalled()))
            .sendTime(row.getSendTime())
            .build();
    }

    private MessageDto.MessageResponse toMessageResponse(Message message, String senderName) {
        return MessageDto.MessageResponse.builder()
            .messageId(message.getMessageId())
            .senderId(message.getSenderId())
            .senderName(senderName)
            .receiverId(message.getReceiverId())
            .groupId(message.getGroupId())
            .messageType(toMessageTypeName(message.getMessageType()))
            .content(displayContent(message.getContent(), message.getIsRecalled()))
            .recalled(isRecalled(message.getIsRecalled()))
            .sendTime(message.getSendTime())
            .build();
    }

    private MessageDto.MessageSessionResponse buildSessionResponse(
        String sessionType,
        Long targetId,
        String targetName,
        MessageDto.MessageResponse lastMessage
    ) {
        return MessageDto.MessageSessionResponse.builder()
            .sessionType(sessionType)
            .targetId(targetId)
            .targetName(targetName)
            .lastMessage(lastMessage)
            .lastTime(lastMessage == null ? null : lastMessage.getSendTime())
            .build();
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "content is required");
        }
        return content.strip();
    }

    private int toMessageTypeCode(String messageType) {
        if (!StringUtils.hasText(messageType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "messageType is required");
        }
        return switch (messageType.trim().toLowerCase(Locale.ROOT)) {
            case "text" -> MESSAGE_TYPE_TEXT;
            case "emoji" -> MESSAGE_TYPE_EMOJI;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "messageType must be text or emoji");
        };
    }

    private String toMessageTypeName(Integer messageType) {
        if (Integer.valueOf(MESSAGE_TYPE_EMOJI).equals(messageType)) {
            return "emoji";
        }
        return "text";
    }

    private String displayContent(String content, Integer recalled) {
        return isRecalled(recalled) ? RECALLED_CONTENT : content;
    }

    private boolean isRecalled(Integer recalled) {
        return Integer.valueOf(MESSAGE_RECALLED).equals(recalled);
    }

    private String displayName(User user) {
        return displayNameOrFallback(user, user == null ? null : user.getUserId());
    }

    private String displayNameOrFallback(User user, Long fallbackId) {
        if (user != null && StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (user != null && StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return fallbackId == null ? "用户" : "用户#" + fallbackId;
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // 没有事务时无法等待提交，直接执行；当前消息写入路径通常会进入下面的 afterCommit 分支。
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 只在数据库真正提交后才广播，避免前端收到一条随后被事务回滚的“幽灵消息”。
                action.run();
            }
        });
    }

    private record FriendPair(Long userId, Long friendUserId) {

        private static FriendPair of(Long firstUserId, Long secondUserId) {
            return firstUserId < secondUserId
                ? new FriendPair(firstUserId, secondUserId)
                : new FriendPair(secondUserId, firstUserId);
        }
    }
}
