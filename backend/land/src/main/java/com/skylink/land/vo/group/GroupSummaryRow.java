package com.skylink.land.vo.group;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GroupSummaryRow {

    private Long groupId;

    private String groupName;

    private String avatar;

    private String notice;

    private Long ownerId;

    private String ownerName;

    private Integer memberCount;

    private LocalDateTime createTime;
}
