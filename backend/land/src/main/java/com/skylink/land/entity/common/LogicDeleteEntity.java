package com.skylink.land.entity.common;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class LogicDeleteEntity extends AuditEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
