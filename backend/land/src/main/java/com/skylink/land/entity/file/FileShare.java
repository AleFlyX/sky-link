package com.skylink.land.entity.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.CreateTimeEntity;
import java.io.Serial;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("file_share")
@EqualsAndHashCode(callSuper = true)
public class FileShare extends CreateTimeEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "share_id", type = IdType.AUTO)
    private Long shareId;

    private Long fileId;

    private Long targetUserId;

    private Long targetGroupId;

    private Integer permissionType;

    private LocalDateTime expireTime;
}
