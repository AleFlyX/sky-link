package com.skylink.land.service.identity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skylink.land.entity.identity.Department;
import com.skylink.land.vo.identity.DepartmentVO;
import java.util.List;

public interface DepartmentService extends IService<Department> {

    DepartmentVO getDepartmentVO(Long departmentId);

    List<DepartmentVO> listDepartmentVO();
}
