package com.skylink.land.dto.task;

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
public final class TaskDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTaskRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private String content;
        private Long executorId;
        private Integer priority;
        private LocalDateTime deadline;
        private List<Long> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTaskRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private String content;
        private Long executorId;
        private Integer priority;
        private LocalDateTime deadline;
        private List<Long> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class TaskQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;

        private String status;
        private Integer priority;
        private Long executorId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTaskStatusRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long taskId;
        private String title;
        private String content;
        private String remark;
        private String status;
        private Integer priority;
        private Integer progressRate;
        private LocalDateTime deadline;
        private LocalDateTime startTime;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private UserDto.UserSummaryResponse creator;
        private UserDto.UserSummaryResponse executor;
        private List<TaskAttachmentInfo> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskAttachmentInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long attachmentId;
        private Long fileId;
        private String fileName;
        private String filePath;
        private Long fileSize;
        private String fileType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStatisticsResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Integer total;
        private Integer completed;
        private Integer inProgress;
        private Integer notStarted;
        private Double completionRate;
    }
}
