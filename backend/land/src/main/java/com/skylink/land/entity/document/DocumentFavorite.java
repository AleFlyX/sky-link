package com.skylink.land.entity.document;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skylink.land.entity.common.CreateTimeEntity;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("document_favorite")
@EqualsAndHashCode(callSuper = true)
public class DocumentFavorite extends CreateTimeEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long documentId;
}
