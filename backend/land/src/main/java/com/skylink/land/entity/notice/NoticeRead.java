package com.skylink.land.entity.notice;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("notice_read")
public class NoticeRead implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long noticeId;

    private Long userId;

    @TableField("read_time")
    private LocalDateTime readTime;
}
