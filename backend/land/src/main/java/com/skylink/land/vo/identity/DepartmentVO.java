package com.skylink.land.vo.identity;

import com.skylink.land.entity.identity.Department;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long departmentId;

    private String departmentName;

    private Long leaderId;

    private String leaderName;

    private String description;

    private Integer memberCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static DepartmentVO from(Department department) {
        if (department == null) {
            return null;
        }
        return DepartmentVO.builder()
            .departmentId(department.getDepartmentId())
            .departmentName(department.getDepartmentName())
            .leaderId(department.getLeaderId())
            .description(department.getDescription())
            .createTime(department.getCreateTime())
            .updateTime(department.getUpdateTime())
            .build();
    }
}
