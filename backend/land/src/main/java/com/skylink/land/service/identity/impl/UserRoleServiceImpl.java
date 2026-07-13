package com.skylink.land.service.identity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skylink.land.entity.identity.UserRole;
import com.skylink.land.mapper.identity.UserRoleMapper;
import com.skylink.land.service.identity.UserRoleService;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

    @Override
    public List<Long> listRoleIdsByUserId(Long userId) {
        return list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)).stream()
            .map(UserRole::getRoleId)
            .toList();
    }

    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        return list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId)).stream()
            .map(UserRole::getUserId)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceUserRoles(Long userId, List<Long> roleIds) {
        remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }

        List<UserRole> userRoles = roleIds.stream()
            .distinct()
            .map(roleId -> buildUserRole(userId, roleId))
            .toList();
        saveBatch(userRoles);
    }

    private UserRole buildUserRole(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }
}
