package com.skylink.land.vo.identity;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserProfileVO extends UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<RoleVO> roles;

    private List<String> permissions;
}
