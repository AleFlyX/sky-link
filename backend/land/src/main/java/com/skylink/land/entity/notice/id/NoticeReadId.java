package com.skylink.land.entity.notice.id;

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
public class NoticeReadId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long noticeId;

    private Long userId;
}
