package com.skylink.land.dto.file;

import com.skylink.land.dto.common.PageRequest;
import com.skylink.land.dto.user.UserDto;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadFileRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private byte[] file;
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class FileQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;

        private String fileType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfoResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long fileId;
        private String fileName;
        private String filePath;
        private Long fileSize;
        private String fileType;
        private String mimeType;
        private Long ownerId;
        private String ownerName;
        private LocalDateTime uploadTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareFileRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private List<Long> targetUserIds;
        private Long targetGroupId;
        private String permission;
        private LocalDateTime expireTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileShareResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long shareId;
        private Long fileId;
        private String fileName;
        private String permission;
        private String shareUrl;
        private LocalDateTime expireTime;
        private Long targetGroupId;
        private List<Long> targetUserIds;
        private LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class FileShareQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedFileItemResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long shareId;
        private FileInfoResponse file;
        private UserDto.UserSummaryResponse sharer;
        private String permission;
        private String shareUrl;
        private LocalDateTime expireTime;
        private LocalDateTime createTime;
    }
}
