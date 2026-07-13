package com.skylink.land.entity.file.id;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FileFavoriteId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long fileId;
}
