package com.skylink.land.entity.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.CreateTimeEntity;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("task_attachment")
@EqualsAndHashCode(callSuper = true)
public class TaskAttachment extends CreateTimeEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "attachment_id", type = IdType.AUTO)
    private Long attachmentId;

    private Long taskId;

    private Long fileId;
}
