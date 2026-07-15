package com.skylink.land.controller;

import com.skylink.land.auth.AuthContext;
import com.skylink.land.auth.RequirePermission;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.user.UserDto;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.service.identity.UserService;
import com.skylink.land.vo.identity.RoleVO;
import com.skylink.land.vo.identity.UserProfileVO;
import com.skylink.land.vo.identity.UserVO;
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
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @RequirePermission("user:me:get")
    public UserDto.UserProfileResponse me() {
        return toProfileResponse(userService.getUserProfile(AuthContext.requireUserId()));
    }

    @PutMapping("/me")
    @RequirePermission("user:me:update")
    public UserDto.UserProfileResponse updateProfile(@RequestBody UserDto.UpdateProfileRequest request) {
        return toProfileResponse(userService.updateProfile(AuthContext.requireUserId(), request));
    }

    @PutMapping("/me/password")
    @RequirePermission("user:password:update")
    public void changePassword(@RequestBody UserDto.ChangePasswordRequest request) {
        userService.changePassword(AuthContext.requireUserId(), request);
    }

    @GetMapping
    @RequirePermission("user:list")
    public PageResponse<UserDto.UserSummaryResponse> pageUsers(UserDto.UserQueryRequest request) {
        PageResponse<UserVO> page = userService.pageUsers(request);
        return PageResponse.<UserDto.UserSummaryResponse>builder()
            .total(page.getTotal())
            .page(page.getPage())
            .size(page.getSize())
            .records(page.getRecords().stream().map(this::toUserSummaryResponse).toList())
            .build();
    }

    @PutMapping("/{userId}/status")
    @RequirePermission("user:status:update")
    public UserDto.UserSummaryResponse updateUserStatus(
        @PathVariable Long userId,
        @RequestBody UserDto.UpdateUserStatusRequest request
    ) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        return toUserSummaryResponse(userService.updateUserStatus(userId, request.getStatus()));
    }

    @DeleteMapping("/{userId}")
    @RequirePermission("user:delete")
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }

    @PostMapping("/{userId}/roles")
    @RequirePermission("user:role:add")
    public List<UserDto.UserRoleInfo> assignUserRoles(
        @PathVariable Long userId,
        @RequestBody UserDto.AssignUserRolesRequest request
    ) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        return toRoleInfos(userService.assignRoles(userId, request.getRoleIds()));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @RequirePermission("user:role:delete")
    public List<UserDto.UserRoleInfo> removeUserRole(@PathVariable Long userId, @PathVariable Long roleId) {
        return toRoleInfos(userService.removeRole(userId, roleId));
    }

    private UserDto.UserProfileResponse toProfileResponse(UserProfileVO profile) {
        if (profile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        return UserDto.UserProfileResponse.builder()
            .userId(profile.getUserId())
            .username(profile.getUsername())
            .nickname(profile.getNickname())
            .email(profile.getEmail())
            .phone(profile.getPhone())
            .status(profile.getStatus())
            .departmentId(profile.getDepartmentId())
            .departmentName(profile.getDepartmentName())
            .createTime(profile.getCreateTime())
            .roles(toRoleInfos(profile.getRoles()))
            .permissions(profile.getPermissions())
            .build();
    }

    private List<UserDto.UserRoleInfo> toRoleInfos(List<RoleVO> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
            .map(role -> UserDto.UserRoleInfo.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .roleCode(role.getRoleCode())
                .build())
            .toList();
    }

    private UserDto.UserSummaryResponse toUserSummaryResponse(UserVO user) {
        return UserDto.UserSummaryResponse.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .email(user.getEmail())
            .phone(user.getPhone())
            .status(user.getStatus())
            .departmentId(user.getDepartmentId())
            .departmentName(user.getDepartmentName())
            .createTime(user.getCreateTime())
            .build();
    }
}
