package com.skylink.land.entity.document;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.LogicDeleteEntity;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("document")
@EqualsAndHashCode(callSuper = true)
public class Document extends LogicDeleteEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "document_id", type = IdType.AUTO)
    private Long documentId;

    private String title;

    private String content;

    private Long creatorId;

    private Integer status;
}
