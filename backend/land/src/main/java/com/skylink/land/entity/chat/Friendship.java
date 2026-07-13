package com.skylink.land.entity.chat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.AuditEntity;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("friendship")
@EqualsAndHashCode(callSuper = true)
public class Friendship extends AuditEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long friendUserId;

    private Integer status;

    private Long initiatorId;
}
