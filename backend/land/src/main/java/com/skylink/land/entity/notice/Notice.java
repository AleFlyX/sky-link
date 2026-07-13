package com.skylink.land.entity.notice;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.LogicDeleteEntity;
import java.io.Serial;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("notice")
@EqualsAndHashCode(callSuper = true)
public class Notice extends LogicDeleteEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "notice_id", type = IdType.AUTO)
    private Long noticeId;

    private String title;

    private String content;

    private Long publisherId;

    private Integer noticeType;

    private Integer status;

    private LocalDateTime publishTime;
}
