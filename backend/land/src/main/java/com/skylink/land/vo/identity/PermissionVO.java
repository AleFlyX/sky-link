package com.skylink.land.vo.identity;

import com.skylink.land.entity.identity.Permission;
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
public class PermissionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long permissionId;

    private String permissionName;

    private String permissionCode;

    private Integer permissionType;

    private Integer sortNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static PermissionVO from(Permission permission) {
        if (permission == null) {
            return null;
        }
        return PermissionVO.builder()
            .permissionId(permission.getPermissionId())
            .permissionName(permission.getPermissionName())
            .permissionCode(permission.getPermissionCode())
            .permissionType(permission.getPermissionType())
            .sortNo(permission.getSortNo())
            .createTime(permission.getCreateTime())
            .updateTime(permission.getUpdateTime())
            .build();
    }
}
