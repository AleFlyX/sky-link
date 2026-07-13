package com.skylink.land.controller;

import com.skylink.land.auth.AuthContext;
import com.skylink.land.dto.common.ApiResponse;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.friend.FriendDto;
import com.skylink.land.service.chat.FriendService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/requests")
    public ApiResponse<FriendDto.FriendRequestResultResponse> createRequest(
        @RequestBody FriendDto.CreateFriendRequest request
    ) {
        return ApiResponse.success("request sent", friendService.createRequest(AuthContext.requireUserId(), request));
    }

    @PutMapping("/requests/{requestId}")
    public ApiResponse<FriendDto.HandleFriendResponse> handleRequest(
        @PathVariable Long requestId,
        @RequestBody FriendDto.HandleFriendRequest request
    ) {
        String action = request == null ? null : request.getAction();
        String message = "accept".equalsIgnoreCase(action)
            ? "friend request accepted"
            : "friend request rejected";
        return ApiResponse.success(message, friendService.handleRequest(AuthContext.requireUserId(), requestId, request));
    }

    @GetMapping
    public PageResponse<FriendDto.FriendItemResponse> listFriends(FriendDto.FriendListQueryRequest request) {
        return friendService.listFriends(AuthContext.requireUserId(), request);
    }

    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> deleteFriend(@PathVariable Long friendId) {
        friendService.deleteFriend(AuthContext.requireUserId(), friendId);
        return ApiResponse.success("friend deleted", null);
    }

    @GetMapping("/requests")
    public PageResponse<FriendDto.FriendRequestItemResponse> listIncomingRequests(
        FriendDto.FriendRequestQueryRequest request
    ) {
        return friendService.listIncomingRequests(AuthContext.requireUserId(), request);
    }
}
