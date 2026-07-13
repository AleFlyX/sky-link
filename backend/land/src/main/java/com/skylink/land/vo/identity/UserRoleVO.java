package com.skylink.land.vo.identity;

import com.skylink.land.entity.identity.UserRole;
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
public class UserRoleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long roleId;

    private LocalDateTime createTime;

    public static UserRoleVO from(UserRole userRole) {
        if (userRole == null) {
            return null;
        }
        return UserRoleVO.builder()
            .userId(userRole.getUserId())
            .roleId(userRole.getRoleId())
            .createTime(userRole.getCreateTime())
            .build();
    }
}
