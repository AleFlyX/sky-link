package com.skylink.land.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.friend.FriendDto;
import com.skylink.land.dto.user.UserDto;
import com.skylink.land.entity.chat.Friendship;
import com.skylink.land.entity.identity.User;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.chat.FriendshipMapper;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.service.chat.FriendService;
import com.skylink.land.vo.friend.FriendListRow;
import com.skylink.land.vo.friend.FriendRequestRow;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FriendServiceImpl implements FriendService {

    private static final int STATUS_PENDING = 0;

    private static final int STATUS_ACCEPTED = 1;

    private static final int STATUS_REJECTED = 2;

    private static final int STATUS_DELETED = 3;

    private final FriendshipMapper friendshipMapper;

    private final UserMapper userMapper;

    public FriendServiceImpl(FriendshipMapper friendshipMapper, UserMapper userMapper) {
        this.friendshipMapper = friendshipMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendDto.FriendRequestResultResponse createRequest(Long currentUserId, FriendDto.CreateFriendRequest request) {
        if (request == null || request.getFriendUserId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "friendUserId is required");
        }
        Long targetUserId = request.getFriendUserId();
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cannot add yourself as a friend");
        }

        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "target user not found");
        }

        FriendPair pair = FriendPair.of(currentUserId, targetUserId);
        Friendship existing = friendshipMapper.selectByUsers(pair.userId(), pair.friendUserId());
        if (existing != null) {
            return handleExistingRequest(currentUserId, targetUserId, existing);
        }

        Friendship friendship = new Friendship();
        friendship.setUserId(pair.userId());
        friendship.setFriendUserId(pair.friendUserId());
        friendship.setStatus(STATUS_PENDING);
        friendship.setInitiatorId(currentUserId);
        friendshipMapper.insert(friendship);

        return FriendDto.FriendRequestResultResponse.builder()
            .requestId(currentUserId)
            .status(toStatusName(STATUS_PENDING))
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendDto.HandleFriendResponse handleRequest(
        Long currentUserId,
        Long requestId,
        FriendDto.HandleFriendRequest request
    ) {
        if (requestId == null || request == null || !StringUtils.hasText(request.getAction())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "requestId and action are required");
        }
        if (currentUserId.equals(requestId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cannot handle your own request");
        }

        User requestUser = userMapper.selectById(requestId);
        if (requestUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "request user not found");
        }

        FriendPair pair = FriendPair.of(currentUserId, requestId);
        Friendship friendship = friendshipMapper.selectByUsers(pair.userId(), pair.friendUserId());
        if (friendship == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "friend request not found");
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(friendship.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "friend request has already been processed");
        }
        if (currentUserId.equals(friendship.getInitiatorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "cannot process your own outgoing request");
        }

        String action = request.getAction().trim().toLowerCase();
        int nextStatus = switch (action) {
            case "accept" -> STATUS_ACCEPTED;
            case "reject" -> STATUS_REJECTED;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "action must be accept or reject");
        };

        friendshipMapper.update(
            null,
            new LambdaUpdateWrapper<Friendship>()
                .eq(Friendship::getUserId, pair.userId())
                .eq(Friendship::getFriendUserId, pair.friendUserId())
                .eq(Friendship::getStatus, STATUS_PENDING)
                .set(Friendship::getStatus, nextStatus)
        );

        if (nextStatus != STATUS_ACCEPTED) {
            return null;
        }

        return FriendDto.HandleFriendResponse.builder()
            .friendId(requestId)
            .friendUser(toUserSummary(requestUser))
            .build();
    }

    @Override
    public PageResponse<FriendDto.FriendItemResponse> listFriends(Long currentUserId, FriendDto.FriendListQueryRequest request) {
        FriendDto.FriendListQueryRequest query = request == null ? new FriendDto.FriendListQueryRequest() : request;
        long total = friendshipMapper.countAcceptedFriends(currentUserId, trimToNull(query.getNickname()));
        if (total == 0) {
            return PageResponse.empty(query);
        }

        int page = query.pageOrDefault();
        int size = query.sizeOrDefault();
        long offset = (long) (page - 1) * size;
        List<FriendDto.FriendItemResponse> records = friendshipMapper
            .selectAcceptedFriends(currentUserId, trimToNull(query.getNickname()), offset, size)
            .stream()
            .map(this::toFriendItemResponse)
            .toList();

        return PageResponse.<FriendDto.FriendItemResponse>builder()
            .total(total)
            .page(page)
            .size(size)
            .records(records)
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long currentUserId, Long friendId) {
        if (friendId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "friendId is required");
        }
        if (currentUserId.equals(friendId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cannot delete yourself");
        }

        FriendPair pair = FriendPair.of(currentUserId, friendId);
        Friendship friendship = friendshipMapper.selectByUsers(pair.userId(), pair.friendUserId());
        if (friendship == null || !Integer.valueOf(STATUS_ACCEPTED).equals(friendship.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "friend relationship not found");
        }

        friendshipMapper.softDeleteByUsers(pair.userId(), pair.friendUserId());
    }

    @Override
    public PageResponse<FriendDto.FriendRequestItemResponse> listIncomingRequests(
        Long currentUserId,
        FriendDto.FriendRequestQueryRequest request
    ) {
        FriendDto.FriendRequestQueryRequest query = request == null ? new FriendDto.FriendRequestQueryRequest() : request;
        long total = friendshipMapper.countIncomingRequests(currentUserId);
        if (total == 0) {
            return PageResponse.empty(query);
        }

        int page = query.pageOrDefault();
        int size = query.sizeOrDefault();
        long offset = (long) (page - 1) * size;
        List<FriendDto.FriendRequestItemResponse> records = friendshipMapper.selectIncomingRequests(currentUserId, offset, size)
            .stream()
            .map(this::toFriendRequestItemResponse)
            .toList();

        return PageResponse.<FriendDto.FriendRequestItemResponse>builder()
            .total(total)
            .page(page)
            .size(size)
            .records(records)
            .build();
    }

    private FriendDto.FriendRequestResultResponse handleExistingRequest(
        Long currentUserId,
        Long targetUserId,
        Friendship existing
    ) {
        Integer status = existing.getStatus();
        if (Integer.valueOf(STATUS_ACCEPTED).equals(status)) {
            throw new BusinessException(ErrorCode.CONFLICT, "you are already friends");
        }
        if (Integer.valueOf(STATUS_PENDING).equals(status)) {
            if (currentUserId.equals(existing.getInitiatorId())) {
                throw new BusinessException(ErrorCode.CONFLICT, "friend request already sent");
            }
            throw new BusinessException(ErrorCode.CONFLICT, "incoming friend request is waiting for you");
        }
        friendshipMapper.update(
            null,
            new LambdaUpdateWrapper<Friendship>()
                .eq(Friendship::getUserId, existing.getUserId())
                .eq(Friendship::getFriendUserId, existing.getFriendUserId())
                .set(Friendship::getStatus, STATUS_PENDING)
                .set(Friendship::getInitiatorId, currentUserId)
        );

        return FriendDto.FriendRequestResultResponse.builder()
            .requestId(currentUserId)
            .status(toStatusName(STATUS_PENDING))
            .build();
    }

    private FriendDto.FriendItemResponse toFriendItemResponse(FriendListRow row) {
        return FriendDto.FriendItemResponse.builder()
            .friendId(row.getFriendUserId())
            .friendUser(toUserSummary(row))
            .addTime(row.getAddTime())
            .build();
    }

    private FriendDto.FriendRequestItemResponse toFriendRequestItemResponse(FriendRequestRow row) {
        return FriendDto.FriendRequestItemResponse.builder()
            .requestId(row.getRequestUserId())
            .requestUser(toUserSummary(row))
            .message(null)
            .status(toStatusName(STATUS_PENDING))
            .requestTime(row.getRequestTime())
            .build();
    }

    private UserDto.UserSummaryResponse toUserSummary(User user) {
        return UserDto.UserSummaryResponse.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .avatar(user.getAvatar())
            .email(user.getEmail())
            .phone(user.getPhone())
            .status(user.getStatus())
            .departmentId(user.getDepartmentId())
            .createTime(user.getCreateTime())
            .build();
    }

    private UserDto.UserSummaryResponse toUserSummary(FriendListRow row) {
        return UserDto.UserSummaryResponse.builder()
            .userId(row.getFriendUserId())
            .username(row.getUsername())
            .nickname(row.getNickname())
            .avatar(row.getAvatar())
            .email(row.getEmail())
            .phone(row.getPhone())
            .status(row.getStatus())
            .departmentId(row.getDepartmentId())
            .departmentName(row.getDepartmentName())
            .createTime(row.getCreateTime())
            .build();
    }

    private UserDto.UserSummaryResponse toUserSummary(FriendRequestRow row) {
        return UserDto.UserSummaryResponse.builder()
            .userId(row.getRequestUserId())
            .username(row.getUsername())
            .nickname(row.getNickname())
            .avatar(row.getAvatar())
            .email(row.getEmail())
            .phone(row.getPhone())
            .status(row.getStatus())
            .departmentId(row.getDepartmentId())
            .departmentName(row.getDepartmentName())
            .createTime(row.getCreateTime())
            .build();
    }

    private String toStatusName(Integer status) {
        return switch (status) {
            case STATUS_PENDING -> "pending";
            case STATUS_ACCEPTED -> "accepted";
            case STATUS_REJECTED -> "rejected";
            case STATUS_DELETED -> "deleted";
            default -> "unknown";
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record FriendPair(Long userId, Long friendUserId) {

        private static FriendPair of(Long firstUserId, Long secondUserId) {
            return firstUserId < secondUserId
                ? new FriendPair(firstUserId, secondUserId)
                : new FriendPair(secondUserId, firstUserId);
        }
    }
}
