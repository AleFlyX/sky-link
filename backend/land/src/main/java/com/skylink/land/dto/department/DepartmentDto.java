package com.skylink.land.dto.department;

import com.skylink.land.dto.common.PageRequest;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DepartmentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentResponse implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long departmentId;
        private String departmentName;
        private Long leaderId;
        private String leaderName;
        private String description;
        private Integer memberCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveDepartmentRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String departmentName;
        private Long leaderId;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class DepartmentMemberQueryRequest extends PageRequest {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddDepartmentMembersRequest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private List<Long> userIds;
    }
}
