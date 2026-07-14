package com.skylink.land.service.chat;

import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.group.GroupDto;
import java.util.List;

public interface GroupService {

    GroupDto.GroupDetailResponse createGroup(Long currentUserId, GroupDto.CreateGroupRequest request);

    PageResponse<GroupDto.GroupSummaryResponse> listGroups(Long currentUserId, GroupDto.GroupListQueryRequest request);

    GroupDto.GroupDetailResponse getGroupDetail(Long currentUserId, Long groupId);

    GroupDto.GroupDetailResponse updateGroup(Long currentUserId, Long groupId, GroupDto.UpdateGroupRequest request);

    void dissolveGroup(Long currentUserId, Long groupId);

    PageResponse<GroupDto.GroupMemberResponse> listMembers(Long currentUserId, Long groupId, GroupDto.GroupMemberQueryRequest request);

    List<GroupDto.GroupMemberResponse> inviteMembers(Long currentUserId, Long groupId, GroupDto.InviteGroupMembersRequest request);

    void removeMember(Long currentUserId, Long groupId, Long userId);

    GroupDto.GroupMemberResponse updateMemberRole(
        Long currentUserId,
        Long groupId,
        Long userId,
        GroupDto.UpdateGroupMemberRoleRequest request
    );

    void leaveGroup(Long currentUserId, Long groupId);
}
