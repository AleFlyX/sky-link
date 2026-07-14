package com.skylink.land.vo.group;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GroupMemberRow {

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private Integer memberRole;

    private LocalDateTime joinTime;
}
