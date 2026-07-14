package com.skylink.land.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.friend.FriendDto;
import com.skylink.land.dto.user.UserDto;
import com.skylink.land.entity.chat.FriendRequest;
import com.skylink.land.entity.chat.Friendship;
import com.skylink.land.entity.identity.User;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.chat.FriendRequestMapper;
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

    private final FriendRequestMapper friendRequestMapper;

    private final FriendshipMapper friendshipMapper;

    private final UserMapper userMapper;

    public FriendServiceImpl(
        FriendRequestMapper friendRequestMapper,
        FriendshipMapper friendshipMapper,
        UserMapper userMapper
    ) {
        this.friendRequestMapper = friendRequestMapper;
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
        if (friendshipMapper.selectByUsers(pair.userId(), pair.friendUserId()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "you are already friends");
        }

        FriendRequest pendingRequest = friendRequestMapper.selectPendingBetween(currentUserId, targetUserId);
        if (pendingRequest != null) {
            if (currentUserId.equals(pendingRequest.getRequesterId())) {
                throw new BusinessException(ErrorCode.CONFLICT, "friend request already sent");
            }
            throw new BusinessException(ErrorCode.CONFLICT, "incoming friend request is waiting for you");
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setRequesterId(currentUserId);
        friendRequest.setReceiverId(targetUserId);
        friendRequest.setMessage(trimToNull(request.getMessage()));
        friendRequest.setStatus(STATUS_PENDING);
        friendRequestMapper.insert(friendRequest);

        return FriendDto.FriendRequestResultResponse.builder()
            .requestId(friendRequest.getRequestId())
            .status(toStatusName(friendRequest.getStatus()))
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

        FriendRequest friendRequest = friendRequestMapper.selectById(requestId);
        if (friendRequest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "friend request not found");
        }
        if (!currentUserId.equals(friendRequest.getReceiverId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only the receiver can process this friend request");
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(friendRequest.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "friend request has already been processed");
        }

        String action = request.getAction().trim().toLowerCase();
        int nextStatus = switch (action) {
            case "accept" -> STATUS_ACCEPTED;
            case "reject" -> STATUS_REJECTED;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "action must be accept or reject");
        };

        int updated = friendRequestMapper.update(
            null,
            new LambdaUpdateWrapper<FriendRequest>()
                .eq(FriendRequest::getRequestId, requestId)
                .eq(FriendRequest::getStatus, STATUS_PENDING)
                .set(FriendRequest::getStatus, nextStatus)
        );
        if (updated != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "friend request has already been processed");
        }
        if (nextStatus == STATUS_REJECTED) {
            return null;
        }

        User requester = userMapper.selectById(friendRequest.getRequesterId());
        if (requester == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "request user not found");
        }

        FriendPair pair = FriendPair.of(friendRequest.getRequesterId(), friendRequest.getReceiverId());
        if (friendshipMapper.selectByUsers(pair.userId(), pair.friendUserId()) == null) {
            Friendship friendship = new Friendship();
            friendship.setUserId(pair.userId());
            friendship.setFriendUserId(pair.friendUserId());
            friendshipMapper.insert(friendship);
        }

        return FriendDto.HandleFriendResponse.builder()
            .friendUserId(friendRequest.getRequesterId())
            .friendUser(toUserSummary(requester))
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
    public void deleteFriend(Long currentUserId, Long friendUserId) {
        if (friendUserId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "friendUserId is required");
        }
        if (currentUserId.equals(friendUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cannot delete yourself");
        }

        FriendPair pair = FriendPair.of(currentUserId, friendUserId);
        if (friendshipMapper.deleteByUsers(pair.userId(), pair.friendUserId()) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "friend relationship not found");
        }
    }

    @Override
    public PageResponse<FriendDto.FriendRequestItemResponse> listIncomingRequests(
        Long currentUserId,
        FriendDto.FriendRequestQueryRequest request
    ) {
        FriendDto.FriendRequestQueryRequest query = request == null ? new FriendDto.FriendRequestQueryRequest() : request;
        long total = friendRequestMapper.countIncomingRequests(currentUserId);
        if (total == 0) {
            return PageResponse.empty(query);
        }

        int page = query.pageOrDefault();
        int size = query.sizeOrDefault();
        long offset = (long) (page - 1) * size;
        List<FriendDto.FriendRequestItemResponse> records = friendRequestMapper
            .selectIncomingRequests(currentUserId, offset, size)
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

    private FriendDto.FriendItemResponse toFriendItemResponse(FriendListRow row) {
        return FriendDto.FriendItemResponse.builder()
            .friendId(row.getFriendUserId())
            .friendUser(toUserSummary(row))
            .addTime(row.getAddTime())
            .build();
    }

    private FriendDto.FriendRequestItemResponse toFriendRequestItemResponse(FriendRequestRow row) {
        return FriendDto.FriendRequestItemResponse.builder()
            .requestId(row.getRequestId())
            .requestUser(toUserSummary(row))
            .message(row.getMessage())
            .status(toStatusName(row.getRequestStatus()))
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
        if (status == null) {
            return "unknown";
        }
        return switch (status) {
            case STATUS_PENDING -> "pending";
            case STATUS_ACCEPTED -> "accepted";
            case STATUS_REJECTED -> "rejected";
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
