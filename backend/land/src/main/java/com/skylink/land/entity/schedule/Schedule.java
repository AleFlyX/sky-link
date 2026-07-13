package com.skylink.land.entity.schedule;

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
@TableName("schedule")
@EqualsAndHashCode(callSuper = true)
public class Schedule extends LogicDeleteEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "schedule_id", type = IdType.AUTO)
    private Long scheduleId;

    private String title;

    private String content;

    private Long userId;

    private Integer scheduleType;

    private Integer isAllDay;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime remindTime;

    private Integer repeatType;
}
