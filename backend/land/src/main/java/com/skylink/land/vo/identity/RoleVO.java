package com.skylink.land.vo.identity;

import com.skylink.land.entity.identity.Role;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long roleId;

    private String roleName;

    private String roleCode;

    private String description;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<PermissionVO> permissions;

    public static RoleVO from(Role role) {
        if (role == null) {
            return null;
        }
        return RoleVO.builder()
            .roleId(role.getRoleId())
            .roleName(role.getRoleName())
            .roleCode(role.getRoleCode())
            .description(role.getDescription())
            .status(role.getStatus())
            .createTime(role.getCreateTime())
            .updateTime(role.getUpdateTime())
            .build();
    }
}
