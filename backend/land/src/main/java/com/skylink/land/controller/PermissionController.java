package com.skylink.land.controller;

import com.skylink.land.auth.RequirePermission;
import com.skylink.land.dto.admin.AdminDto;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.identity.IdentityDto;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.service.identity.PermissionService;
import com.skylink.land.vo.identity.PermissionVO;
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
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @RequirePermission("permission:list")
    public List<AdminDto.PermissionResponse> listPermissions() {
        return permissionService.listPermissions().stream()
            .map(this::toPermissionResponse)
            .toList();
    }

    @GetMapping("/page")
    @RequirePermission("permission:list")
    public PageResponse<AdminDto.PermissionResponse> pagePermissions(IdentityDto.PermissionQueryRequest request) {
        PageResponse<PermissionVO> page = permissionService.pagePermissions(request);
        return PageResponse.<AdminDto.PermissionResponse>builder()
            .total(page.getTotal())
            .page(page.getPage())
            .size(page.getSize())
            .records(page.getRecords().stream().map(this::toPermissionResponse).toList())
            .build();
    }

    @GetMapping("/{permissionId}")
    @RequirePermission("permission:list")
    public AdminDto.PermissionResponse getPermission(@PathVariable Long permissionId) {
        return toPermissionResponse(permissionService.getPermissionVO(permissionId));
    }

    @PostMapping
    @RequirePermission("permission:create")
    public AdminDto.PermissionResponse createPermission(@RequestBody IdentityDto.SavePermissionRequest request) {
        return toPermissionResponse(permissionService.createPermission(request));
    }

    @PutMapping("/{permissionId}")
    @RequirePermission("permission:update")
    public AdminDto.PermissionResponse updatePermission(
        @PathVariable Long permissionId,
        @RequestBody IdentityDto.SavePermissionRequest request
    ) {
        return toPermissionResponse(permissionService.updatePermission(permissionId, request));
    }

    @DeleteMapping("/{permissionId}")
    @RequirePermission("permission:delete")
    public void deletePermission(@PathVariable Long permissionId) {
        permissionService.deletePermission(permissionId);
    }

    private AdminDto.PermissionResponse toPermissionResponse(PermissionVO permission) {
        if (permission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "permission not found");
        }
        return AdminDto.PermissionResponse.builder()
            .permissionId(permission.getPermissionId())
            .permissionName(permission.getPermissionName())
            .permissionCode(permission.getPermissionCode())
            .permissionType(permission.getPermissionType())
            .sortNo(permission.getSortNo())
            .build();
    }
}
