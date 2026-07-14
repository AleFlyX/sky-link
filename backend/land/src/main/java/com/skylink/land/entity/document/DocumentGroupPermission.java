package com.skylink.land.entity.document;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.CreateTimeEntity;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("document_group_permission")
@EqualsAndHashCode(callSuper = true)
public class DocumentGroupPermission extends CreateTimeEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long documentId;

    private Long groupId;

    private Integer permissionType;
}
