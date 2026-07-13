package com.skylink.land.service.identity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.identity.IdentityDto;
import com.skylink.land.entity.identity.Permission;
import com.skylink.land.entity.identity.Role;
import com.skylink.land.entity.identity.RolePermission;
import com.skylink.land.entity.identity.UserRole;
import com.skylink.land.mapper.identity.PermissionMapper;
import com.skylink.land.mapper.identity.RoleMapper;
import com.skylink.land.mapper.identity.RolePermissionMapper;
import com.skylink.land.mapper.identity.UserRoleMapper;
import com.skylink.land.service.identity.RoleService;
import com.skylink.land.vo.identity.PermissionVO;
import com.skylink.land.vo.identity.RoleVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final UserRoleMapper userRoleMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final PermissionMapper permissionMapper;

    public RoleServiceImpl(
        UserRoleMapper userRoleMapper,
        RolePermissionMapper rolePermissionMapper,
        PermissionMapper permissionMapper
    ) {
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public PageResponse<RoleVO> pageRoles(IdentityDto.RoleQueryRequest request) {
        IdentityDto.RoleQueryRequest query = request == null ? new IdentityDto.RoleQueryRequest() : request;
        Page<Role> page = page(
            query.toMybatisPage(),
            new LambdaQueryWrapper<Role>()
                .like(StringUtils.hasText(query.getRoleName()), Role::getRoleName, query.getRoleName())
                .like(StringUtils.hasText(query.getRoleCode()), Role::getRoleCode, query.getRoleCode())
                .eq(query.getStatus() != null, Role::getStatus, query.getStatus())
                .orderByDesc(Role::getCreateTime)
        );
        return PageResponse.of(page.convert(this::toRoleVO));
    }

    @Override
    public RoleVO getRoleVO(Long roleId) {
        return toRoleVO(getById(roleId));
    }

    @Override
    public List<RoleVO> listByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
            ).stream()
            .map(UserRole::getRoleId)
            .distinct()
            .toList();
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return baseMapper.selectBatchIds(roleIds).stream()
            .map(this::toRoleVO)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        permissionIds.stream()
            .distinct()
            .map(permissionId -> buildRolePermission(roleId, permissionId))
            .forEach(rolePermissionMapper::insert);
    }

    private RoleVO toRoleVO(Role role) {
        RoleVO roleVO = RoleVO.from(role);
        if (roleVO == null) {
            return null;
        }
        roleVO.setPermissions(listPermissions(role.getRoleId()));
        return roleVO;
    }

    private List<PermissionVO> listPermissions(Long roleId) {
        List<Long> permissionIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
            ).stream()
            .map(RolePermission::getPermissionId)
            .distinct()
            .toList();
        if (CollectionUtils.isEmpty(permissionIds)) {
            return List.of();
        }
        return permissionMapper.selectBatchIds(permissionIds).stream()
            .map(PermissionVO::from)
            .toList();
    }

    private RolePermission buildRolePermission(Long roleId, Long permissionId) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        return rolePermission;
    }
}
