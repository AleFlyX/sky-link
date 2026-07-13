package com.skylink.land.service.identity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skylink.land.entity.identity.RolePermission;
import java.util.Collection;
import java.util.List;

public interface RolePermissionService extends IService<RolePermission> {

    List<Long> listPermissionIdsByRoleId(Long roleId);

    List<Long> listPermissionIdsByRoleIds(Collection<Long> roleIds);

    void replaceRolePermissions(Long roleId, List<Long> permissionIds);
}
