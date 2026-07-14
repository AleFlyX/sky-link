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
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.identity.PermissionMapper;
import com.skylink.land.mapper.identity.RoleMapper;
import com.skylink.land.mapper.identity.RolePermissionMapper;
import com.skylink.land.mapper.identity.UserRoleMapper;
import com.skylink.land.service.identity.RoleService;
import com.skylink.land.vo.identity.PermissionVO;
import com.skylink.land.vo.identity.RoleVO;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    @Transactional(rollbackFor = Exception.class)
    public RoleVO createRole(IdentityDto.SaveRoleRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        requireText("roleName", request.getRoleName());
        requireText("roleCode", request.getRoleCode());
        validateStatus(request.getStatus());

        String roleName = request.getRoleName().trim();
        String roleCode = request.getRoleCode().trim();
        ensureUniqueRoleName(roleName, null);
        ensureUniqueRoleCode(roleCode, null);

        Role role = new Role();
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setDescription(trimToNull(request.getDescription()));
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        save(role);

        if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
            assignPermissions(role.getRoleId(), request.getPermissionIds());
        }
        return getRoleVO(role.getRoleId());
    }

    @Override
    public RoleVO getRoleVO(Long roleId) {
        return toRoleVO(getById(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO updateRole(Long roleId, IdentityDto.SaveRoleRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        Role role = requireRole(roleId);

        if (request.getRoleName() != null) {
            requireText("roleName", request.getRoleName());
            String roleName = request.getRoleName().trim();
            ensureUniqueRoleName(roleName, roleId);
            role.setRoleName(roleName);
        }
        if (request.getRoleCode() != null) {
            requireText("roleCode", request.getRoleCode());
            String roleCode = request.getRoleCode().trim();
            ensureUniqueRoleCode(roleCode, roleId);
            role.setRoleCode(roleCode);
        }
        if (request.getDescription() != null) {
            role.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getStatus() != null) {
            validateStatus(request.getStatus());
            role.setStatus(request.getStatus());
        }

        updateById(role);
        if (request.getPermissionIds() != null) {
            assignPermissions(roleId, request.getPermissionIds());
        }
        return getRoleVO(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        requireRole(roleId);
        Long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
        if (userCount != null && userCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "role is assigned to users");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        removeById(roleId);
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
        requireRole(roleId);
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        List<Long> normalizedPermissionIds = normalizeIds("permissionIds", permissionIds);
        if (CollectionUtils.isEmpty(normalizedPermissionIds)) {
            return;
        }
        ensurePermissionsExist(normalizedPermissionIds);
        normalizedPermissionIds.stream()
            .map(permissionId -> buildRolePermission(roleId, permissionId))
            .forEach(rolePermissionMapper::insert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO assignPermissionCodes(Long roleId, List<String> permissionCodes) {
        requireRole(roleId);
        List<String> normalizedCodes = normalizeCodes(permissionCodes);
        if (CollectionUtils.isEmpty(normalizedCodes)) {
            assignPermissions(roleId, List.of());
            return getRoleVO(roleId);
        }

        List<Permission> permissions = permissionMapper.selectList(
            new LambdaQueryWrapper<Permission>().in(Permission::getPermissionCode, normalizedCodes)
        );
        Set<String> foundCodes = permissions.stream()
            .map(Permission::getPermissionCode)
            .collect(java.util.stream.Collectors.toSet());
        List<String> missingCodes = normalizedCodes.stream()
            .filter(code -> !foundCodes.contains(code))
            .toList();
        if (!missingCodes.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "permissions not found: " + String.join(", ", missingCodes));
        }

        assignPermissions(roleId, permissions.stream().map(Permission::getPermissionId).toList());
        return getRoleVO(roleId);
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

    private Role requireRole(Long roleId) {
        if (roleId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "roleId is required");
        }
        Role role = getById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "role not found");
        }
        return role;
    }

    private void ensurePermissionsExist(List<Long> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        List<Long> foundIds = permissionMapper.selectBatchIds(permissionIds).stream()
            .map(Permission::getPermissionId)
            .toList();
        List<Long> missingIds = permissionIds.stream()
            .filter(permissionId -> !foundIds.contains(permissionId))
            .toList();
        if (!missingIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "permissions not found: " + missingIds);
        }
    }

    private List<Long> normalizeIds(String fieldName, List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        if (ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " contains invalid id");
        }
        return ids.stream()
            .distinct()
            .toList();
    }

    private List<String> normalizeCodes(List<String> permissionCodes) {
        if (CollectionUtils.isEmpty(permissionCodes)) {
            return List.of();
        }
        if (permissionCodes.stream().anyMatch(code -> !StringUtils.hasText(code))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "permissionCodes contains blank code");
        }
        return permissionCodes.stream()
            .map(String::trim)
            .collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                List::copyOf
            ));
    }

    private void ensureUniqueRoleName(String roleName, Long excludeRoleId) {
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleName, roleName)
                .ne(excludeRoleId != null, Role::getRoleId, excludeRoleId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "roleName already exists");
        }
    }

    private void ensureUniqueRoleCode(String roleCode, Long excludeRoleId) {
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, roleCode)
                .ne(excludeRoleId != null, Role::getRoleId, excludeRoleId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "roleCode already exists");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status must be 0 or 1");
        }
    }

    private void requireText(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " cannot be blank");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
