package com.skylink.land.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.group.GroupDto;
import com.skylink.land.entity.chat.ChatGroup;
import com.skylink.land.entity.chat.GroupMember;
import com.skylink.land.entity.identity.User;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.chat.ChatGroupMapper;
import com.skylink.land.mapper.chat.GroupMemberMapper;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.service.chat.GroupService;
import com.skylink.land.vo.group.GroupMemberRow;
import com.skylink.land.vo.group.GroupSummaryRow;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class GroupServiceImpl implements GroupService {

    private static final int GROUP_STATUS_NORMAL = 1;

    private static final int ROLE_OWNER = 1;

    private static final int ROLE_ADMIN = 2;

    private static final int ROLE_MEMBER = 3;

    private static final int ROLE_EXITED = 4;

    private final ChatGroupMapper chatGroupMapper;

    private final GroupMemberMapper groupMemberMapper;

    private final UserMapper userMapper;

    public GroupServiceImpl(ChatGroupMapper chatGroupMapper, GroupMemberMapper groupMemberMapper, UserMapper userMapper) {
        this.chatGroupMapper = chatGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupDto.GroupDetailResponse createGroup(Long currentUserId, GroupDto.CreateGroupRequest request) {
        if (request == null || !StringUtils.hasText(request.getGroupName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "groupName is required");
        }

        ChatGroup group = new ChatGroup();
        group.setGroupName(request.getGroupName().trim());
        group.setNotice(trimToNull(request.getNotice()));
        group.setOwnerId(currentUserId);
        group.setStatus(GROUP_STATUS_NORMAL);
        // 先写群主表记录，insert 后 MyBatis-Plus 会把生成的 groupId 回填到 group 中。
        chatGroupMapper.insert(group);

        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getGroupId());
        ownerMember.setUserId(currentUserId);
        // 创建者既是群表 ownerId，也是成员表中的 owner；两处数据共同支撑群内权限判断。
        ownerMember.setMemberRole(ROLE_OWNER);
        groupMemberMapper.insert(ownerMember);

        // 初始成员沿用邀请逻辑，避免“建群时邀请”和“后续邀请”出现两套不一致规则。
        inviteMembersInternal(group.getGroupId(), normalizeUserIds(request.getMemberIds()), currentUserId);
        return buildGroupDetailResponse(group.getGroupId());
    }

    @Override
    public PageResponse<GroupDto.GroupSummaryResponse> listGroups(Long currentUserId, GroupDto.GroupListQueryRequest request) {
        GroupDto.GroupListQueryRequest query = request == null ? new GroupDto.GroupListQueryRequest() : request;
        long total = chatGroupMapper.countJoinedGroups(currentUserId);
        if (total == 0) {
            return PageResponse.empty(query);
        }

        int page = query.pageOrDefault();
        int size = query.sizeOrDefault();
        long offset = (long) (page - 1) * size;
        List<GroupDto.GroupSummaryResponse> records = chatGroupMapper.selectJoinedGroups(currentUserId, offset, size)
            .stream()
            .map(this::toGroupSummaryResponse)
            .toList();

        return PageResponse.<GroupDto.GroupSummaryResponse>builder()
            .total(total)
            .page(page)
            .size(size)
            .records(records)
            .build();
    }

    @Override
    public GroupDto.GroupDetailResponse getGroupDetail(Long currentUserId, Long groupId) {
        // 查看详情也要先确认是该群的有效成员，不能只凭 groupId 枚举别人群聊。
        requireActiveMember(groupId, currentUserId);
        return buildGroupDetailResponse(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupDto.GroupDetailResponse updateGroup(Long currentUserId, Long groupId, GroupDto.UpdateGroupRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }

        // 群资料由群主或管理员维护；普通成员只有查看权。
        requireAdminOrOwner(groupId, currentUserId);
        ChatGroup group = getActiveGroup(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "group not found");
        }

        boolean changed = false;
        if (request.getGroupName() != null) {
            requireText("groupName", request.getGroupName());
            group.setGroupName(request.getGroupName().trim());
            changed = true;
        }
        if (request.getNotice() != null) {
            group.setNotice(trimToNull(request.getNotice()));
            changed = true;
        }

        if (changed) {
            chatGroupMapper.updateById(group);
        }
        return buildGroupDetailResponse(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long currentUserId, Long groupId) {
        // 解散是群的最终操作，只授权给 owner，不授权给普通管理员。
        requireOwner(groupId, currentUserId);
        ChatGroup group = getActiveGroup(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "group not found");
        }

        // 先让所有成员关系失效，再删群；事务保证不会留下“群已删但成员仍有效”的中间状态。
        groupMemberMapper.deactivateAllMembers(groupId);
        chatGroupMapper.deleteById(groupId);
    }

    @Override
    public PageResponse<GroupDto.GroupMemberResponse> listMembers(
        Long currentUserId,
        Long groupId,
        GroupDto.GroupMemberQueryRequest request
    ) {
        requireActiveMember(groupId, currentUserId);
        GroupDto.GroupMemberQueryRequest query = request == null ? new GroupDto.GroupMemberQueryRequest() : request;
        long total = groupMemberMapper.countActiveMembers(groupId);
        if (total == 0) {
            return PageResponse.empty(query);
        }

        int page = query.pageOrDefault();
        int size = query.sizeOrDefault();
        long offset = (long) (page - 1) * size;
        List<GroupDto.GroupMemberResponse> records = groupMemberMapper.selectActiveMembers(groupId, offset, size)
            .stream()
            .map(this::toGroupMemberResponse)
            .toList();

        return PageResponse.<GroupDto.GroupMemberResponse>builder()
            .total(total)
            .page(page)
            .size(size)
            .records(records)
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<GroupDto.GroupMemberResponse> inviteMembers(
        Long currentUserId,
        Long groupId,
        GroupDto.InviteGroupMembersRequest request
    ) {
        // 邀请属于群内管理动作，管理员和群主均可执行。
        requireAdminOrOwner(groupId, currentUserId);
        getActiveGroupOrThrow(groupId);
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        List<Long> invitedUserIds = inviteMembersInternal(groupId, normalizeUserIds(request.getUserIds()), currentUserId);
        if (CollectionUtils.isEmpty(invitedUserIds)) {
            return List.of();
        }

        Map<Long, GroupMemberRow> memberRowMap = groupMemberMapper.selectAllActiveMembers(groupId).stream()
            .filter(row -> invitedUserIds.contains(row.getUserId()))
            .collect(Collectors.toMap(GroupMemberRow::getUserId, Function.identity()));

        return invitedUserIds.stream()
            .map(memberRowMap::get)
            .filter(Objects::nonNull)
            .map(this::toGroupMemberResponse)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long currentUserId, Long groupId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userId is required");
        }
        if (currentUserId.equals(userId)) {
            // 自己退出应走专门接口，便于应用“群主不能退出”的独立规则。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "use leave endpoint to quit the group");
        }

        GroupMember operator = requireAdminOrOwner(groupId, currentUserId);
        GroupMember target = getActiveMembership(groupId, userId);
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "group member not found");
        }
        if (Integer.valueOf(ROLE_OWNER).equals(target.getMemberRole())) {
            // 无论操作人是谁，都不允许移除群主，避免群聊失去所有者。
            throw new BusinessException(ErrorCode.FORBIDDEN, "cannot remove the group owner");
        }
        if (Integer.valueOf(ROLE_ADMIN).equals(operator.getMemberRole()) && !Integer.valueOf(ROLE_MEMBER).equals(target.getMemberRole())) {
            // 管理员只能管理普通成员，不能移除同级管理员或越权处理更高角色。
            throw new BusinessException(ErrorCode.FORBIDDEN, "admin can only remove normal members");
        }

        groupMemberMapper.deactivateMember(groupId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupDto.GroupMemberResponse updateMemberRole(
        Long currentUserId,
        Long groupId,
        Long userId,
        GroupDto.UpdateGroupMemberRoleRequest request
    ) {
        if (userId == null || request == null || !StringUtils.hasText(request.getRole())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userId and role are required");
        }

        // 提升或降级管理员会改变管理边界，因此只允许群主执行。
        requireOwner(groupId, currentUserId);
        GroupMember membership = getActiveMembership(groupId, userId);
        if (membership == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "group member not found");
        }
        if (Integer.valueOf(ROLE_OWNER).equals(membership.getMemberRole())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cannot modify owner role");
        }

        int nextRole = switch (request.getRole().trim().toLowerCase()) {
            case "admin" -> ROLE_ADMIN;
            case "member" -> ROLE_MEMBER;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "role must be admin or member");
        };

        groupMemberMapper.update(
            null,
            new LambdaUpdateWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .set(GroupMember::getMemberRole, nextRole)
        );

        return groupMemberMapper.selectAllActiveMembers(groupId).stream()
            .filter(row -> userId.equals(row.getUserId()))
            .findFirst()
            .map(this::toGroupMemberResponse)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "group member not found"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long currentUserId, Long groupId) {
        GroupMember membership = requireActiveMember(groupId, currentUserId);
        if (Integer.valueOf(ROLE_OWNER).equals(membership.getMemberRole())) {
            // 不能让群主直接退出；当前实现要求先解散群，以保持所有权模型完整。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "group owner cannot leave directly, dissolve the group instead");
        }

        groupMemberMapper.deactivateMember(groupId, currentUserId);
    }

    private List<Long> inviteMembersInternal(Long groupId, List<Long> userIds, Long operatorUserId) {
        if (CollectionUtils.isEmpty(userIds)) {
            return List.of();
        }

        List<Long> filteredUserIds = userIds.stream()
            .filter(Objects::nonNull)
            .filter(userId -> !userId.equals(operatorUserId))
            .toList();
        if (CollectionUtils.isEmpty(filteredUserIds)) {
            return List.of();
        }

        List<User> users = userMapper.selectBatchIds(filteredUserIds);
        if (users.size() != filteredUserIds.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "some users do not exist");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Long> invitedUserIds = new ArrayList<>();
        for (Long userId : filteredUserIds) {
            GroupMember existing = groupMemberMapper.selectMembership(groupId, userId);
            if (existing == null) {
                GroupMember member = new GroupMember();
                member.setGroupId(groupId);
                member.setUserId(userId);
                member.setMemberRole(ROLE_MEMBER);
                member.setJoinTime(now);
                groupMemberMapper.insert(member);
                invitedUserIds.add(userId);
                continue;
            }
            if (Integer.valueOf(ROLE_EXITED).equals(existing.getMemberRole())) {
                groupMemberMapper.update(
                    null,
                    new LambdaUpdateWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .eq(GroupMember::getUserId, userId)
                        .set(GroupMember::getMemberRole, ROLE_MEMBER)
                        .set(GroupMember::getJoinTime, now)
                );
                invitedUserIds.add(userId);
            }
        }
        return invitedUserIds;
    }

    private GroupDto.GroupDetailResponse buildGroupDetailResponse(Long groupId) {
        ChatGroup group = getActiveGroup(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "group not found");
        }

        User owner = userMapper.selectById(group.getOwnerId());
        List<GroupMemberRow> members = groupMemberMapper.selectAllActiveMembers(groupId);
        return GroupDto.GroupDetailResponse.builder()
            .groupId(group.getGroupId())
            .groupName(group.getGroupName())
            .notice(group.getNotice())
            .ownerId(group.getOwnerId())
            .ownerName(resolveDisplayName(owner))
            .memberCount(members.size())
            .createTime(group.getCreateTime())
            .members(members.stream().map(this::toGroupMemberResponse).toList())
            .build();
    }

    private ChatGroup getActiveGroupOrThrow(Long groupId) {
        ChatGroup group = getActiveGroup(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "group not found");
        }
        return group;
    }

    private ChatGroup getActiveGroup(Long groupId) {
        if (groupId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "groupId is required");
        }
        return chatGroupMapper.selectOne(
            new LambdaQueryWrapper<ChatGroup>()
                .eq(ChatGroup::getGroupId, groupId)
                .eq(ChatGroup::getStatus, GROUP_STATUS_NORMAL)
                .last("limit 1")
        );
    }

    private GroupMember requireOwner(Long groupId, Long currentUserId) {
        GroupMember membership = requireActiveMember(groupId, currentUserId);
        if (!Integer.valueOf(ROLE_OWNER).equals(membership.getMemberRole())) {
            // 这里查的是“当前群中的角色”，不是用户在其他群里的身份。
            throw new BusinessException(ErrorCode.FORBIDDEN, "only group owner can perform this operation");
        }
        return membership;
    }

    private GroupMember requireAdminOrOwner(Long groupId, Long currentUserId) {
        GroupMember membership = requireActiveMember(groupId, currentUserId);
        if (!Integer.valueOf(ROLE_OWNER).equals(membership.getMemberRole())
            && !Integer.valueOf(ROLE_ADMIN).equals(membership.getMemberRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only group owner or admin can perform this operation");
        }
        return membership;
    }

    private GroupMember requireActiveMember(Long groupId, Long currentUserId) {
        // 先确认群仍存在且正常，再确认用户在这个群中仍有有效成员关系。
        getActiveGroupOrThrow(groupId);
        GroupMember membership = getActiveMembership(groupId, currentUserId);
        if (membership == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "you are not a member of this group");
        }
        return membership;
    }

    private GroupMember getActiveMembership(Long groupId, Long userId) {
        GroupMember membership = groupMemberMapper.selectMembership(groupId, userId);
        if (membership == null) {
            return null;
        }
        return switch (membership.getMemberRole()) {
            case ROLE_OWNER, ROLE_ADMIN, ROLE_MEMBER -> membership;
            default -> null;
        };
    }

    private GroupDto.GroupSummaryResponse toGroupSummaryResponse(GroupSummaryRow row) {
        return GroupDto.GroupSummaryResponse.builder()
            .groupId(row.getGroupId())
            .groupName(row.getGroupName())
            .notice(row.getNotice())
            .ownerId(row.getOwnerId())
            .ownerName(row.getOwnerName())
            .memberCount(row.getMemberCount())
            .createTime(row.getCreateTime())
            .build();
    }

    private GroupDto.GroupMemberResponse toGroupMemberResponse(GroupMemberRow row) {
        return GroupDto.GroupMemberResponse.builder()
            .userId(row.getUserId())
            .username(row.getUsername())
            .nickname(row.getNickname())
            .role(toRoleName(row.getMemberRole()))
            .joinTime(row.getJoinTime())
            .build();
    }

    private String toRoleName(Integer role) {
        return switch (role) {
            case ROLE_OWNER -> "owner";
            case ROLE_ADMIN -> "admin";
            case ROLE_MEMBER -> "member";
            case ROLE_EXITED -> "exited";
            default -> "unknown";
        };
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void requireText(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " cannot be blank");
        }
    }

    private List<Long> normalizeUserIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return List.of();
        }
        Set<Long> deduplicated = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null) {
                deduplicated.add(userId);
            }
        }
        return List.copyOf(deduplicated);
    }
}
