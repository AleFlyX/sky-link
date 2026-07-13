package com.skylink.land.dto.common;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer page;

    private Integer size;
}
