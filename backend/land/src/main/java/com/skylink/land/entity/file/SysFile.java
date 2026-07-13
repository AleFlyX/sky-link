package com.skylink.land.entity.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_file")
public class SysFile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "file_id", type = IdType.AUTO)
    private Long fileId;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileExt;

    private String mimeType;

    private Long ownerId;

    private Integer storageType;

    @TableField("upload_time")
    private LocalDateTime uploadTime;

    @TableField("is_deleted")
    @TableLogic
    private Integer deleted;
}
