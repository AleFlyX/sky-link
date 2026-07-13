package com.skylink.land.vo.identity;

import com.skylink.land.entity.identity.RolePermission;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long roleId;

    private Long permissionId;

    private LocalDateTime createTime;

    public static RolePermissionVO from(RolePermission rolePermission) {
        if (rolePermission == null) {
            return null;
        }
        return RolePermissionVO.builder()
            .roleId(rolePermission.getRoleId())
            .permissionId(rolePermission.getPermissionId())
            .createTime(rolePermission.getCreateTime())
            .build();
    }
}
