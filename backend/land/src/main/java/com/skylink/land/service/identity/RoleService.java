package com.skylink.land.service.identity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.identity.IdentityDto;
import com.skylink.land.entity.identity.Role;
import com.skylink.land.vo.identity.RoleVO;
import java.util.List;

public interface RoleService extends IService<Role> {

    PageResponse<RoleVO> pageRoles(IdentityDto.RoleQueryRequest request);

    RoleVO getRoleVO(Long roleId);

    List<RoleVO> listByUserId(Long userId);

    void assignPermissions(Long roleId, List<Long> permissionIds);
}
