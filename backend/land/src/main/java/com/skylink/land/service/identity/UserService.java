package com.skylink.land.service.identity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.user.UserDto;
import com.skylink.land.entity.identity.User;
import com.skylink.land.vo.identity.UserProfileVO;
import com.skylink.land.vo.identity.UserVO;
import com.skylink.land.vo.identity.RoleVO;
import java.util.List;

public interface UserService extends IService<User> {

    PageResponse<UserVO> pageUsers(UserDto.UserQueryRequest request);

    UserVO getUserVO(Long userId);

    UserVO updateUserStatus(Long userId, Integer status);

    void deleteUser(Long userId);

    UserProfileVO getUserProfile(Long userId);

    UserProfileVO updateProfile(Long userId, UserDto.UpdateProfileRequest request);

    void changePassword(Long userId, UserDto.ChangePasswordRequest request);

    List<String> listRoleCodes(Long userId);

    List<String> listPermissionCodes(Long userId);

    List<RoleVO> assignRoles(Long userId, List<Long> roleIds);

    List<RoleVO> removeRole(Long userId, Long roleId);
}
