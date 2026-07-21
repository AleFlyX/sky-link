package com.skylink.land.service.identity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.common.PageRequest;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    public PageResponse<DepartmentVO> pageDepartments(DepartmentDto.DepartmentQueryRequest request) {
        DepartmentDto.DepartmentQueryRequest query = request == null
            ? new DepartmentDto.DepartmentQueryRequest()
            : request;

        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            Set<Long> matchedLeaderIds = findLeaderIdsByKeyword(keyword);
            wrapper.and(nested -> {
                nested.like(Department::getDepartmentName, keyword)
                    .or()
                    .like(Department::getDescription, keyword);
                if (!matchedLeaderIds.isEmpty()) {
                    nested.or().in(Department::getLeaderId, matchedLeaderIds);
                }
            });
        }
        wrapper.orderByAsc(Department::getDepartmentId);

        Page<Department> page = page(query.toMybatisPage(), wrapper);
        return PageResponse.of(page.convert(this::toDepartmentVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO createDepartment(DepartmentDto.SaveDepartmentRequest request) {
        if (request == null || !StringUtils.hasText(request.getDepartmentName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "departmentName is required");
        }
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
            // 有成员时拒绝删除，避免用户记录指向已不存在的部门。
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResponse<UserVO> addDepartmentMembers(Long departmentId, List<Long> userIds) {
        Department department = getById(departmentId);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "department not found");
        }

        Set<Long> normalizedUserIds = normalizeUserIds(userIds);
        List<User> users = userMapper.selectBatchIds(normalizedUserIds);
        if (users.size() != normalizedUserIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "some users do not exist");
        }

        for (User user : users) {
            // 加入部门本质是更新用户的 departmentId；同一用户只会有一个当前所属部门。
            userMapper.update(
                null,
                new LambdaUpdateWrapper<User>()
                    .eq(User::getUserId, user.getUserId())
                    .set(User::getDepartmentId, departmentId)
            );
        }

        DepartmentDto.DepartmentMemberQueryRequest request = new DepartmentDto.DepartmentMemberQueryRequest();
        request.setPage(PageRequest.DEFAULT_PAGE);
        request.setSize(Math.max(PageRequest.DEFAULT_SIZE, normalizedUserIds.size()));
        return pageDepartmentMembers(departmentId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDepartmentMember(Long departmentId, Long userId) {
        Department department = getById(departmentId);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "department not found");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userId is required");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        if (!departmentId.equals(user.getDepartmentId())) {
            // 不允许用任意 departmentId 把其他部门的人“移除”，必须先确认其确实属于当前部门。
            throw new BusinessException(ErrorCode.CONFLICT, "user is not in this department");
        }

        userMapper.update(
            null,
            new LambdaUpdateWrapper<User>()
                .eq(User::getUserId, userId)
                .set(User::getDepartmentId, null)
        );
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

    private void validateLeaderId(Long leaderId) {
        if (leaderId != null && userMapper.selectById(leaderId) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "leader user does not exist");
        }
    }

    private Set<Long> findLeaderIdsByKeyword(String keyword) {
        return userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .select(User::getUserId)
                .and(wrapper -> wrapper
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword))
        ).stream().map(User::getUserId).collect(java.util.stream.Collectors.toSet());
    }

    private Set<Long> normalizeUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userIds are required");
        }

        Set<Long> normalizedUserIds = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "userId cannot be null");
            }
            normalizedUserIds.add(userId);
        }
        if (normalizedUserIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userIds are required");
        }
        return normalizedUserIds;
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
