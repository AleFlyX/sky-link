package com.skylink.land.service.identity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.identity.IdentityDto;
import com.skylink.land.entity.identity.Permission;
import com.skylink.land.entity.identity.RolePermission;
import com.skylink.land.entity.identity.UserRole;
import com.skylink.land.mapper.identity.PermissionMapper;
import com.skylink.land.mapper.identity.RolePermissionMapper;
import com.skylink.land.mapper.identity.UserRoleMapper;
import com.skylink.land.service.identity.PermissionService;
import com.skylink.land.vo.identity.PermissionVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final RolePermissionMapper rolePermissionMapper;

    private final UserRoleMapper userRoleMapper;

    public PermissionServiceImpl(RolePermissionMapper rolePermissionMapper, UserRoleMapper userRoleMapper) {
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public PageResponse<PermissionVO> pagePermissions(IdentityDto.PermissionQueryRequest request) {
        IdentityDto.PermissionQueryRequest query = request == null ? new IdentityDto.PermissionQueryRequest() : request;
        Page<Permission> page = page(
            query.toMybatisPage(),
            new LambdaQueryWrapper<Permission>()
                .like(StringUtils.hasText(query.getPermissionName()), Permission::getPermissionName, query.getPermissionName())
                .like(StringUtils.hasText(query.getPermissionCode()), Permission::getPermissionCode, query.getPermissionCode())
                .eq(query.getPermissionType() != null, Permission::getPermissionType, query.getPermissionType())
                .eq(query.getParentId() != null, Permission::getParentId, query.getParentId())
                .orderByAsc(Permission::getSortNo)
                .orderByAsc(Permission::getPermissionId)
        );
        return PageResponse.of(page.convert(PermissionVO::from));
    }

    @Override
    public PermissionVO getPermissionVO(Long permissionId) {
        return PermissionVO.from(getById(permissionId));
    }

    @Override
    public List<PermissionVO> listByRoleId(Long roleId) {
        List<Long> permissionIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
            ).stream()
            .map(RolePermission::getPermissionId)
            .distinct()
            .toList();
        return listByPermissionIds(permissionIds);
    }

    @Override
    public List<PermissionVO> listByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
            .stream()
            .map(UserRole::getRoleId)
            .distinct()
            .toList();
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds)
            ).stream()
            .map(RolePermission::getPermissionId)
            .distinct()
            .toList();
        return listByPermissionIds(permissionIds);
    }

    @Override
    public List<PermissionVO> listPermissionTree() {
        List<PermissionVO> permissions = list(
            new LambdaQueryWrapper<Permission>()
                .orderByAsc(Permission::getSortNo)
                .orderByAsc(Permission::getPermissionId)
        ).stream()
            .map(PermissionVO::from)
            .toList();

        Map<Long, PermissionVO> permissionMap = new LinkedHashMap<>();
        permissions.forEach(permission -> {
            permission.setChildren(new ArrayList<>());
            permissionMap.put(permission.getPermissionId(), permission);
        });

        List<PermissionVO> roots = new ArrayList<>();
        permissions.forEach(permission -> {
            PermissionVO parent = permissionMap.get(permission.getParentId());
            if (parent == null) {
                roots.add(permission);
                return;
            }
            parent.getChildren().add(permission);
        });
        sortTree(roots);
        return roots;
    }

    private List<PermissionVO> listByPermissionIds(List<Long> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return List.of();
        }
        return baseMapper.selectBatchIds(permissionIds).stream()
            .map(PermissionVO::from)
            .toList();
    }

    private void sortTree(List<PermissionVO> permissions) {
        permissions.sort(Comparator
            .comparing((PermissionVO permission) -> permission.getSortNo() == null ? 0 : permission.getSortNo())
            .thenComparing(PermissionVO::getPermissionId));
        permissions.forEach(permission -> sortTree(permission.getChildren()));
    }
}
