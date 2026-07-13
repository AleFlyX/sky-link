package com.skylink.land.entity.identity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.CreateTimeEntity;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user_role")
@EqualsAndHashCode(callSuper = true)
public class UserRole extends CreateTimeEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long roleId;
}
