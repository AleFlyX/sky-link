package com.skylink.land.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.LogicDeleteEntity;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("chat_group")
@EqualsAndHashCode(callSuper = true)
public class ChatGroup extends LogicDeleteEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "group_id", type = IdType.AUTO)
    private Long groupId;

    private String groupName;

    private Long ownerId;

    private String avatar;

    private String notice;

    private Integer status;
}
