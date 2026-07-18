package com.skylink.land.controller;

import com.skylink.land.auth.AuthContext;
import com.skylink.land.dto.common.ApiResponse;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.group.GroupDto;
import com.skylink.land.service.chat.GroupService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ApiResponse<GroupDto.GroupDetailResponse> createGroup(@RequestBody GroupDto.CreateGroupRequest request) {
        // 创建人只从当前登录上下文读取，Service 会同时创建群记录和群主成员记录。
        return ApiResponse.success("group created", groupService.createGroup(AuthContext.requireUserId(), request));
    }

    @GetMapping
    public PageResponse<GroupDto.GroupSummaryResponse> listGroups(GroupDto.GroupListQueryRequest request) {
        return groupService.listGroups(AuthContext.requireUserId(), request);
    }

    @GetMapping("/{groupId}")
    public GroupDto.GroupDetailResponse getGroupDetail(@PathVariable Long groupId) {
        return groupService.getGroupDetail(AuthContext.requireUserId(), groupId);
    }

    @PutMapping("/{groupId}")
    public ApiResponse<GroupDto.GroupDetailResponse> updateGroup(
        @PathVariable Long groupId,
        @RequestBody GroupDto.UpdateGroupRequest request
    ) {
        return ApiResponse.success("group updated", groupService.updateGroup(AuthContext.requireUserId(), groupId, request));
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> dissolveGroup(@PathVariable Long groupId) {
        // 是否为群主不是 Controller 的猜测，而由 Service 查询该群内成员角色后决定。
        groupService.dissolveGroup(AuthContext.requireUserId(), groupId);
        return ApiResponse.success("group dissolved", null);
    }

    @GetMapping("/{groupId}/members")
    public PageResponse<GroupDto.GroupMemberResponse> listMembers(
        @PathVariable Long groupId,
        GroupDto.GroupMemberQueryRequest request
    ) {
        return groupService.listMembers(AuthContext.requireUserId(), groupId, request);
    }

    @PostMapping("/{groupId}/members")
    public ApiResponse<List<GroupDto.GroupMemberResponse>> inviteMembers(
        @PathVariable Long groupId,
        @RequestBody GroupDto.InviteGroupMembersRequest request
    ) {
        return ApiResponse.success("members invited", groupService.inviteMembers(AuthContext.requireUserId(), groupId, request));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.removeMember(AuthContext.requireUserId(), groupId, userId);
        return ApiResponse.success("member removed", null);
    }

    @PutMapping("/{groupId}/members/{userId}/role")
    public ApiResponse<GroupDto.GroupMemberResponse> updateMemberRole(
        @PathVariable Long groupId,
        @PathVariable Long userId,
        @RequestBody GroupDto.UpdateGroupMemberRoleRequest request
    ) {
        return ApiResponse.success(
            "member role updated",
            groupService.updateMemberRole(AuthContext.requireUserId(), groupId, userId, request)
        );
    }

    @DeleteMapping("/{groupId}/members/me")
    public ApiResponse<Void> leaveGroup(@PathVariable Long groupId) {
        // 群主不能直接退出，否则会留下没有所有者的群；Service 会给出明确的业务错误。
        groupService.leaveGroup(AuthContext.requireUserId(), groupId);
        return ApiResponse.success("left group", null);
    }
}
