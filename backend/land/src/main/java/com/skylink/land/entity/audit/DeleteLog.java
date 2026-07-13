package com.skylink.land.entity.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("delete_log")
public class DeleteLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "delete_log_id", type = IdType.AUTO)
    private Long deleteLogId;

    private Long userId;

    private String targetType;

    private Long targetId;

    private String reason;

    @TableField("delete_time")
    private LocalDateTime deleteTime;
}
