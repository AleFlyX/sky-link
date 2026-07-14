package com.skylink.land.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.AuditEntity;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("friend_request")
@EqualsAndHashCode(callSuper = true)
public class FriendRequest extends AuditEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "request_id", type = IdType.AUTO)
    private Long requestId;

    private Long requesterId;

    private Long receiverId;

    private String message;

    private Integer status;
}
