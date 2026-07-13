package com.skylink.land.entity.common;

import java.io.Serial;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class AuditEntity extends CreateTimeEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDateTime updateTime;
}
