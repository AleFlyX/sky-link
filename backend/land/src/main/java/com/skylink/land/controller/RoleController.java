package com.skylink.land.controller;

import com.skylink.land.auth.RequirePermission;
import com.skylink.land.dto.admin.AdminDto;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.identity.IdentityDto;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.service.identity.RoleService;
import com.skylink.land.vo.identity.PermissionVO;
import com.skylink.land.vo.identity.RoleVO;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @RequirePermission("role:list")
    public PageResponse<AdminDto.RoleResponse> pageRoles(IdentityDto.RoleQueryRequest request) {
        PageResponse<RoleVO> page = roleService.pageRoles(request);
        return PageResponse.<AdminDto.RoleResponse>builder()
            .total(page.getTotal())
            .page(page.getPage())
            .size(page.getSize())
            .records(page.getRecords().stream().map(this::toRoleResponse).toList())
            .build();
    }

    @PostMapping
    @RequirePermission("role:create")
    public AdminDto.RoleResponse createRole(@RequestBody AdminDto.CreateRoleRequest request) {
        return toRoleResponse(roleService.createRole(toSaveRoleRequest(request)));
    }

    @PutMapping("/{roleId}")
    @RequirePermission("role:update")
    public AdminDto.RoleResponse updateRole(
        @PathVariable Long roleId,
        @RequestBody AdminDto.UpdateRoleRequest request
    ) {
        return toRoleResponse(roleService.updateRole(roleId, toSaveRoleRequest(request)));
    }

    @DeleteMapping("/{roleId}")
    @RequirePermission("role:delete")
    public void deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
    }

    @PutMapping("/{roleId}/permissions")
    @RequirePermission("role:permission:set")
    public AdminDto.RoleResponse assignRolePermissions(
        @PathVariable Long roleId,
        @RequestBody AdminDto.AssignRolePermissionsRequest request
    ) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        return toRoleResponse(roleService.assignPermissionCodes(roleId, request.getPermissionCodes()));
    }

    private IdentityDto.SaveRoleRequest toSaveRoleRequest(AdminDto.CreateRoleRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        return IdentityDto.SaveRoleRequest.builder()
            .roleName(request.getRoleName())
            .roleCode(request.getRoleCode())
            .description(request.getDescription())
            .build();
    }

    private IdentityDto.SaveRoleRequest toSaveRoleRequest(AdminDto.UpdateRoleRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        return IdentityDto.SaveRoleRequest.builder()
            .roleName(request.getRoleName())
            .roleCode(request.getRoleCode())
            .description(request.getDescription())
            .status(request.getStatus())
            .build();
    }

    private AdminDto.RoleResponse toRoleResponse(RoleVO role) {
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "role not found");
        }
        return AdminDto.RoleResponse.builder()
            .roleId(role.getRoleId())
            .roleName(role.getRoleName())
            .roleCode(role.getRoleCode())
            .description(role.getDescription())
            .status(role.getStatus())
            .permissions(toPermissionResponses(role.getPermissions()))
            .build();
    }

    private List<AdminDto.PermissionResponse> toPermissionResponses(List<PermissionVO> permissions) {
        if (permissions == null) {
            return List.of();
        }
        return permissions.stream().map(this::toPermissionResponse).toList();
    }

    private AdminDto.PermissionResponse toPermissionResponse(PermissionVO permission) {
        return AdminDto.PermissionResponse.builder()
            .permissionId(permission.getPermissionId())
            .permissionName(permission.getPermissionName())
            .permissionCode(permission.getPermissionCode())
            .permissionType(permission.getPermissionType())
            .sortNo(permission.getSortNo())
            .build();
    }
}
