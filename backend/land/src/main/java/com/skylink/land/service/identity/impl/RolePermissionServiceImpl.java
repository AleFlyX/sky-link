package com.skylink.land.service.identity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skylink.land.entity.identity.RolePermission;
import com.skylink.land.mapper.identity.RolePermissionMapper;
import com.skylink.land.service.identity.RolePermissionService;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class RolePermissionServiceImpl
    extends ServiceImpl<RolePermissionMapper, RolePermission>
    implements RolePermissionService {

    @Override
    public List<Long> listPermissionIdsByRoleId(Long roleId) {
        return list(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)).stream()
            .map(RolePermission::getPermissionId)
            .toList();
    }

    @Override
    public List<Long> listPermissionIdsByRoleIds(Collection<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds)).stream()
            .map(RolePermission::getPermissionId)
            .distinct()
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
        remove(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }

        List<RolePermission> rolePermissions = permissionIds.stream()
            .distinct()
            .map(permissionId -> buildRolePermission(roleId, permissionId))
            .toList();
        saveBatch(rolePermissions);
    }

    private RolePermission buildRolePermission(Long roleId, Long permissionId) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        return rolePermission;
    }
}
