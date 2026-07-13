package com.skylink.land.dto.schedule;

import com.skylink.land.dto.common.PageRequest;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScheduleDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateScheduleRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private String content;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime remindTime;
        private String repeatType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateScheduleRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private String content;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime remindTime;
        private String repeatType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class ScheduleQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;

        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long scheduleId;
        private String title;
        private String content;
        private Long userId;
        private Integer scheduleType;
        private Boolean allDay;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime remindTime;
        private String repeatType;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
}
