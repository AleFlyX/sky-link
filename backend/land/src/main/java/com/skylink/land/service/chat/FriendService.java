package com.skylink.land.service.chat;

import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.friend.FriendDto;

public interface FriendService {

    FriendDto.FriendRequestResultResponse createRequest(Long currentUserId, FriendDto.CreateFriendRequest request);

    FriendDto.HandleFriendResponse handleRequest(
        Long currentUserId,
        Long requestId,
        FriendDto.HandleFriendRequest request
    );

    PageResponse<FriendDto.FriendItemResponse> listFriends(Long currentUserId, FriendDto.FriendListQueryRequest request);

    void deleteFriend(Long currentUserId, Long friendUserId);

    PageResponse<FriendDto.FriendRequestItemResponse> listIncomingRequests(
        Long currentUserId,
        FriendDto.FriendRequestQueryRequest request
    );

    PageResponse<FriendDto.SentFriendRequestItemResponse> listOutgoingRequests(
        Long currentUserId,
        FriendDto.FriendRequestQueryRequest request
    );
}
