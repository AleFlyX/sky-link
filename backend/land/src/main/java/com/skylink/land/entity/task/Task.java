package com.skylink.land.entity.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.LogicDeleteEntity;
import java.io.Serial;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("task")
@EqualsAndHashCode(callSuper = true)
public class Task extends LogicDeleteEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    private String title;

    private String content;

    private String remark;

    private Long creatorId;

    private Long executorId;

    private Integer priority;

    private Integer status;

    private Integer progressRate;

    private LocalDateTime startTime;

    private LocalDateTime deadline;
}
