package com.skylink.land.service.identity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.department.DepartmentDto;
import com.skylink.land.entity.identity.Department;
import com.skylink.land.entity.identity.User;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.identity.DepartmentMapper;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.service.identity.DepartmentService;
import com.skylink.land.vo.identity.DepartmentVO;
import com.skylink.land.vo.identity.UserVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO createDepartment(DepartmentDto.SaveDepartmentRequest request) {
        if (request == null || !StringUtils.hasText(request.getDepartmentName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "departmentName is required");
        }
        validateParentId(request.getParentId());
        validateLeaderId(request.getLeaderId());
        ensureDepartmentNameUnique(request.getDepartmentName().trim(), null);

        Department department = new Department();
        department.setDepartmentName(request.getDepartmentName().trim());
        department.setLeaderId(request.getLeaderId());
        department.setDescription(normalize(request.getDescription()));
        save(department);
        return getDepartmentVO(department.getDepartmentId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO updateDepartment(Long departmentId, DepartmentDto.SaveDepartmentRequest request) {
        Department department = getById(departmentId);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "department not found");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        validateParentId(request.getParentId());

        if (request.getDepartmentName() != null) {
            if (!StringUtils.hasText(request.getDepartmentName())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "departmentName cannot be blank");
            }
            String departmentName = request.getDepartmentName().trim();
            ensureDepartmentNameUnique(departmentName, departmentId);
            department.setDepartmentName(departmentName);
        }
        if (request.getLeaderId() != null) {
            validateLeaderId(request.getLeaderId());
            department.setLeaderId(request.getLeaderId());
        }
        if (request.getDescription() != null) {
            department.setDescription(normalize(request.getDescription()));
        }

        updateById(department);
        return getDepartmentVO(departmentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long departmentId) {
        Department department = getById(departmentId);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "department not found");
        }

        Long memberCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getDepartmentId, departmentId)
        );
        if (memberCount != null && memberCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "department still has members");
        }

        removeById(departmentId);
    }

    @Override
    public PageResponse<UserVO> pageDepartmentMembers(Long departmentId, DepartmentDto.DepartmentMemberQueryRequest request) {
        Department department = getById(departmentId);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "department not found");
        }

        DepartmentDto.DepartmentMemberQueryRequest query = request == null
            ? new DepartmentDto.DepartmentMemberQueryRequest()
            : request;

        Page<User> page = userMapper.selectPage(
            query.toMybatisPage(),
            new LambdaQueryWrapper<User>()
                .eq(User::getDepartmentId, departmentId)
                .orderByDesc(User::getCreateTime)
        );
        return PageResponse.of(page.convert(this::toUserVO));
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

    private UserVO toUserVO(User user) {
        UserVO userVO = UserVO.from(user);
        if (userVO != null) {
            userVO.setDepartmentName(resolveDepartmentName(user.getDepartmentId()));
        }
        return userVO;
    }

    private void validateParentId(Long parentId) {
        if (parentId != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "parent department is not supported currently");
        }
    }

    private void validateLeaderId(Long leaderId) {
        if (leaderId != null && userMapper.selectById(leaderId) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "leader user does not exist");
        }
    }

    private void ensureDepartmentNameUnique(String departmentName, Long excludeDepartmentId) {
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getDepartmentName, departmentName)
                .ne(excludeDepartmentId != null, Department::getDepartmentId, excludeDepartmentId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "department name already exists");
        }
    }

    private String resolveDepartmentName(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        Department department = baseMapper.selectById(departmentId);
        return department == null ? null : department.getDepartmentName();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
