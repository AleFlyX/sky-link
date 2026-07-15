package com.skylink.land.service.identity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.identity.IdentityDto;
import com.skylink.land.entity.identity.Permission;
import com.skylink.land.entity.identity.RolePermission;
import com.skylink.land.entity.identity.UserRole;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.identity.PermissionMapper;
import com.skylink.land.mapper.identity.RolePermissionMapper;
import com.skylink.land.mapper.identity.UserRoleMapper;
import com.skylink.land.service.identity.PermissionService;
import com.skylink.land.vo.identity.PermissionVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
                .orderByAsc(Permission::getSortNo)
                .orderByAsc(Permission::getPermissionId)
        );
        return PageResponse.of(page.convert(PermissionVO::from));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionVO createPermission(IdentityDto.SavePermissionRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        requireText("permissionName", request.getPermissionName());
        requireText("permissionCode", request.getPermissionCode());
        validatePermissionType(request.getPermissionType());

        String permissionName = request.getPermissionName().trim();
        String permissionCode = request.getPermissionCode().trim();
        ensureUniquePermissionCode(permissionCode, null);

        Permission permission = new Permission();
        permission.setPermissionName(permissionName);
        permission.setPermissionCode(permissionCode);
        permission.setPermissionType(request.getPermissionType() == null ? 1 : request.getPermissionType());
        permission.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        save(permission);
        return getPermissionVO(permission.getPermissionId());
    }

    @Override
    public PermissionVO getPermissionVO(Long permissionId) {
        return PermissionVO.from(getById(permissionId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionVO updatePermission(Long permissionId, IdentityDto.SavePermissionRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        Permission permission = requirePermission(permissionId);

        if (request.getPermissionName() != null) {
            requireText("permissionName", request.getPermissionName());
            permission.setPermissionName(request.getPermissionName().trim());
        }
        if (request.getPermissionCode() != null) {
            requireText("permissionCode", request.getPermissionCode());
            String permissionCode = request.getPermissionCode().trim();
            ensureUniquePermissionCode(permissionCode, permissionId);
            permission.setPermissionCode(permissionCode);
        }
        if (request.getPermissionType() != null) {
            validatePermissionType(request.getPermissionType());
            permission.setPermissionType(request.getPermissionType());
        }
        if (request.getSortNo() != null) {
            permission.setSortNo(request.getSortNo());
        }

        updateById(permission);
        return getPermissionVO(permissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long permissionId) {
        requirePermission(permissionId);
        Long roleCount = rolePermissionMapper.selectCount(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getPermissionId, permissionId)
        );
        if (roleCount != null && roleCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "permission is assigned to roles");
        }
        removeById(permissionId);
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
    public List<PermissionVO> listPermissions() {
        List<PermissionVO> permissions = list(
            new LambdaQueryWrapper<Permission>()
                .orderByAsc(Permission::getSortNo)
                .orderByAsc(Permission::getPermissionId)
        ).stream()
            .map(PermissionVO::from)
            .toList();
        return permissions;
    }

    private List<PermissionVO> listByPermissionIds(List<Long> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return List.of();
        }
        return list(
            new LambdaQueryWrapper<Permission>()
                .in(Permission::getPermissionId, permissionIds)
                .orderByAsc(Permission::getSortNo)
                .orderByAsc(Permission::getPermissionId)
        ).stream()
            .map(PermissionVO::from)
            .toList();
    }

    private Permission requirePermission(Long permissionId) {
        if (permissionId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "permissionId is required");
        }
        Permission permission = getById(permissionId);
        if (permission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "permission not found");
        }
        return permission;
    }

    private void ensureUniquePermissionCode(String permissionCode, Long excludePermissionId) {
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<Permission>()
                .eq(Permission::getPermissionCode, permissionCode)
                .ne(excludePermissionId != null, Permission::getPermissionId, excludePermissionId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "permissionCode already exists");
        }
    }

    private void validatePermissionType(Integer permissionType) {
        if (permissionType != null && permissionType != 1 && permissionType != 2 && permissionType != 3) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "permissionType must be 1, 2 or 3");
        }
    }

    private void requireText(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " cannot be blank");
        }
    }
}
