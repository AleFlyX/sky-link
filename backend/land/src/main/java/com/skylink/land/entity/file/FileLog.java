package com.skylink.land.entity.file;

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
@TableName("file_log")
public class FileLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "file_log_id", type = IdType.AUTO)
    private Long fileLogId;

    private Long fileId;

    private Long userId;

    private Integer actionType;

    private String detail;

    @TableField("action_time")
    private LocalDateTime actionTime;
}
