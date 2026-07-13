package com.skylink.land.controller;

import com.skylink.land.auth.AuthContext;
import com.skylink.land.dto.user.UserDto;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.service.identity.UserService;
import com.skylink.land.vo.identity.RoleVO;
import com.skylink.land.vo.identity.UserProfileVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
    public UserDto.UserProfileResponse me() {
        return toProfileResponse(userService.getUserProfile(AuthContext.requireUserId()));
    }

    @PutMapping("/me")
    public UserDto.UserProfileResponse updateProfile(@RequestBody UserDto.UpdateProfileRequest request) {
        return toProfileResponse(userService.updateProfile(AuthContext.requireUserId(), request));
    }

    @PutMapping("/me/password")
    public void changePassword(@RequestBody UserDto.ChangePasswordRequest request) {
        userService.changePassword(AuthContext.requireUserId(), request);
    }

    private UserDto.UserProfileResponse toProfileResponse(UserProfileVO profile) {
        if (profile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        return UserDto.UserProfileResponse.builder()
            .userId(profile.getUserId())
            .username(profile.getUsername())
            .nickname(profile.getNickname())
            .avatar(profile.getAvatar())
            .email(profile.getEmail())
            .phone(profile.getPhone())
            .status(profile.getStatus())
            .departmentId(profile.getDepartmentId())
            .departmentName(profile.getDepartmentName())
            .createTime(profile.getCreateTime())
            .roles(toRoleInfos(profile.getRoles()))
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
}
