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
@TableName("operation_log")
public class OperationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "operation_id", type = IdType.AUTO)
    private Long operationId;

    private Long userId;

    private String module;

    private String operation;

    private String requestUrl;

    private String requestMethod;

    @TableField("operation_time")
    private LocalDateTime operationTime;
}
