package com.skylink.land.entity.document;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("document_collaboration_state")
public class DocumentCollaborationState implements Serializable {
    @TableId("document_id") private Long documentId;
    private byte[] ydocState;
    private byte[] stateVector;
    private Long revision;
    private Long updatedBy;
    private LocalDateTime updateTime;
}
