package com.skylink.land.entity.common;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CreateTimeEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDateTime createTime;
}
