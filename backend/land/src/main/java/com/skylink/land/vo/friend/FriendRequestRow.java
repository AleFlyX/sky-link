package com.skylink.land.vo.friend;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FriendRequestRow {

    private Long requestId;

    private Long requestUserId;

    private String message;

    private Integer requestStatus;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private Integer status;

    private Long departmentId;

    private String departmentName;

    private LocalDateTime createTime;

    private LocalDateTime requestTime;
}
