package com.skylink.land.vo.identity;

import com.skylink.land.entity.identity.User;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private Integer status;

    private Long departmentId;

    private String departmentName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static UserVO from(User user) {
        if (user == null) {
            return null;
        }
        return UserVO.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .email(user.getEmail())
            .phone(user.getPhone())
            .status(user.getStatus())
            .departmentId(user.getDepartmentId())
            .createTime(user.getCreateTime())
            .updateTime(user.getUpdateTime())
            .build();
    }
}
