package com.skylink.land.service.identity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skylink.land.entity.identity.Department;
import com.skylink.land.entity.identity.User;
import com.skylink.land.mapper.identity.DepartmentMapper;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.service.identity.DepartmentService;
import com.skylink.land.vo.identity.DepartmentVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    private final UserMapper userMapper;

    public DepartmentServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public DepartmentVO getDepartmentVO(Long departmentId) {
        return toDepartmentVO(getById(departmentId));
    }

    @Override
    public List<DepartmentVO> listDepartmentVO() {
        return list(new LambdaQueryWrapper<Department>().orderByAsc(Department::getDepartmentId)).stream()
            .map(this::toDepartmentVO)
            .toList();
    }

    private DepartmentVO toDepartmentVO(Department department) {
        DepartmentVO departmentVO = DepartmentVO.from(department);
        if (departmentVO == null) {
            return null;
        }
        if (department.getLeaderId() != null) {
            User leader = userMapper.selectById(department.getLeaderId());
            if (leader != null) {
                departmentVO.setLeaderName(leader.getNickname());
            }
        }
        Long memberCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getDepartmentId, department.getDepartmentId())
        );
        departmentVO.setMemberCount(Math.toIntExact(memberCount));
        return departmentVO;
    }
}
