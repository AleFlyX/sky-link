package com.skylink.land.vo.friend;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FriendListRow {

    private Long friendUserId;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private Integer status;

    private Long departmentId;

    private String departmentName;

    private LocalDateTime createTime;

    private LocalDateTime addTime;
}
