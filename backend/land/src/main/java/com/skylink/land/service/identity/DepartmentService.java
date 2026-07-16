package com.skylink.land.service.identity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.department.DepartmentDto;
import com.skylink.land.entity.identity.Department;
import com.skylink.land.vo.identity.DepartmentVO;
import com.skylink.land.vo.identity.UserVO;
import java.util.List;

public interface DepartmentService extends IService<Department> {

    DepartmentVO getDepartmentVO(Long departmentId);

    PageResponse<DepartmentVO> pageDepartments(DepartmentDto.DepartmentQueryRequest request);

    DepartmentVO createDepartment(DepartmentDto.SaveDepartmentRequest request);

    DepartmentVO updateDepartment(Long departmentId, DepartmentDto.SaveDepartmentRequest request);

    void deleteDepartment(Long departmentId);

    PageResponse<UserVO> pageDepartmentMembers(Long departmentId, DepartmentDto.DepartmentMemberQueryRequest request);

    PageResponse<UserVO> addDepartmentMembers(Long departmentId, List<Long> userIds);

    void removeDepartmentMember(Long departmentId, Long userId);
}
