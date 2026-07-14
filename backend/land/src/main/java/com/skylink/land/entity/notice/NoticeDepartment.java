package com.skylink.land.entity.notice;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.CreateTimeEntity;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("notice_department")
@EqualsAndHashCode(callSuper = true)
public class NoticeDepartment extends CreateTimeEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long noticeId;

    private Long departmentId;
}
