package com.skylink.land.service.identity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skylink.land.entity.identity.UserRole;
import java.util.List;

public interface UserRoleService extends IService<UserRole> {

    List<Long> listRoleIdsByUserId(Long userId);

    List<Long> listUserIdsByRoleId(Long roleId);

    void replaceUserRoles(Long userId, List<Long> roleIds);
}
