package com.skylink.land.service.identity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.identity.IdentityDto;
import com.skylink.land.entity.identity.Permission;
import com.skylink.land.vo.identity.PermissionVO;
import java.util.List;

public interface PermissionService extends IService<Permission> {

    PageResponse<PermissionVO> pagePermissions(IdentityDto.PermissionQueryRequest request);

    PermissionVO createPermission(IdentityDto.SavePermissionRequest request);

    PermissionVO getPermissionVO(Long permissionId);

    PermissionVO updatePermission(Long permissionId, IdentityDto.SavePermissionRequest request);

    void deletePermission(Long permissionId);

    List<PermissionVO> listByRoleId(Long roleId);

    List<PermissionVO> listByUserId(Long userId);

    List<PermissionVO> listPermissions();
}
