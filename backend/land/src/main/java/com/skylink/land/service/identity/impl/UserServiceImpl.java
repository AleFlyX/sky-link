package com.skylink.land.service.identity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.user.UserDto;
import com.skylink.land.entity.identity.Department;
import com.skylink.land.entity.identity.Permission;
import com.skylink.land.entity.identity.Role;
import com.skylink.land.entity.identity.RolePermission;
import com.skylink.land.entity.identity.User;
import com.skylink.land.entity.identity.UserRole;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.identity.DepartmentMapper;
import com.skylink.land.mapper.identity.PermissionMapper;
import com.skylink.land.mapper.identity.RoleMapper;
import com.skylink.land.mapper.identity.RolePermissionMapper;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.mapper.identity.UserRoleMapper;
import com.skylink.land.service.identity.UserService;
import com.skylink.land.service.identity.bootstrap.SecurityBootstrapCatalog;
import com.skylink.land.vo.identity.RoleVO;
import com.skylink.land.vo.identity.UserProfileVO;
import com.skylink.land.vo.identity.UserVO;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final DepartmentMapper departmentMapper;

    private final RoleMapper roleMapper;

    private final PermissionMapper permissionMapper;

    private final UserRoleMapper userRoleMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
        DepartmentMapper departmentMapper,
        RoleMapper roleMapper,
        PermissionMapper permissionMapper,
        UserRoleMapper userRoleMapper,
        RolePermissionMapper rolePermissionMapper,
        PasswordEncoder passwordEncoder
    ) {
        this.departmentMapper = departmentMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResponse<UserVO> pageUsers(UserDto.UserQueryRequest request) {
        UserDto.UserQueryRequest query = request == null ? new UserDto.UserQueryRequest() : request;
        Page<User> page = page(
            query.toMybatisPage(),
            new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(query.getUsername()), User::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getNickname()), User::getNickname, query.getNickname())
                .eq(query.getDepartmentId() != null, User::getDepartmentId, query.getDepartmentId())
                .eq(query.getStatus() != null, User::getStatus, query.getStatus())
                .orderByDesc(User::getCreateTime)
        );
        return PageResponse.of(page.convert(this::toUserVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO createUser(UserDto.CreateUserRequest request) {
        // 新建用户会同时校验账号、联系方式、部门和默认角色；失败时不留下半成品账号。
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        if (!StringUtils.hasText(request.getUsername())
            || !StringUtils.hasText(request.getPassword())
            || !StringUtils.hasText(request.getEmail())
            || !StringUtils.hasText(request.getPhone())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "username, password, email and phone are required");
        }
        if (!isValidPassword(request.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "password must be at least 8 characters and contain letters and numbers");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();
        String phone = request.getPhone().trim();
        ensureUniqueUsername(username, null);
        ensureUniqueEmail(email, null);
        ensureUniquePhone(phone, null);
        if (request.getDepartmentId() != null) {
            ensureDepartmentExists(request.getDepartmentId());
        }

        Integer status = request.getStatus() == null ? 1 : request.getStatus();
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status must be 0 or 1");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(status);
        user.setDepartmentId(request.getDepartmentId());
        save(user);

        List<Long> roleIds = normalizeIds("roleIds", request.getRoleIds());
        if (CollectionUtils.isEmpty(roleIds)) {
            bindDefaultRole(user.getUserId());
        } else {
            ensureRolesExist(roleIds);
            roleIds.stream()
                .map(roleId -> buildUserRole(user.getUserId(), roleId))
                .forEach(userRoleMapper::insert);
        }

        return getUserVO(user.getUserId());
    }

    @Override
    public UserVO getUserVO(Long userId) {
        return toUserVO(getById(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUserStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status must be 0 or 1");
        }

        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }

        user.setStatus(status);
        updateById(user);
        return getUserVO(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        removeById(userId);
    }

    @Override
    public UserProfileVO getUserProfile(Long userId) {
        UserVO user = getUserVO(userId);
        if (user == null) {
            return null;
        }

        return UserProfileVO.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .email(user.getEmail())
            .phone(user.getPhone())
            .status(user.getStatus())
            .departmentId(user.getDepartmentId())
            .departmentName(user.getDepartmentName())
            .createTime(user.getCreateTime())
            .updateTime(user.getUpdateTime())
            .roles(listRoles(userId))
            .permissions(listPermissionCodes(userId))
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateProfile(Long userId, UserDto.UpdateProfileRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }

        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }

        if (request.getNickname() != null) {
            requireText("nickname", request.getNickname());
            user.setNickname(request.getNickname().trim());
        }
        if (request.getEmail() != null) {
            requireText("email", request.getEmail());
            ensureUniqueEmail(request.getEmail().trim(), userId);
            user.setEmail(request.getEmail().trim());
        }
        if (request.getPhone() != null) {
            requireText("phone", request.getPhone());
            ensureUniquePhone(request.getPhone().trim(), userId);
            user.setPhone(request.getPhone().trim());
        }

        updateById(user);
        return getUserProfile(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, UserDto.ChangePasswordRequest request) {
        if (request == null
            || !StringUtils.hasText(request.getOldPassword())
            || !StringUtils.hasText(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "oldPassword and newPassword are required");
        }
        if (!isValidPassword(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "password must be at least 8 characters and contain letters and numbers");
        }

        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "old password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "new password must be different from old password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateById(user);
    }

    @Override
    public List<String> listRoleCodes(Long userId) {
        List<Role> roles = listEnabledRoles(userId);
        if (CollectionUtils.isEmpty(roles)) {
            return List.of();
        }
        return roles.stream()
            .map(Role::getRoleCode)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
    }

    @Override
    public List<String> listPermissionCodes(Long userId) {
        // 权限不是直接挂在用户身上：先找用户角色，再找角色权限，最后转换成权限码。
        List<Long> roleIds = listEnabledRoles(userId).stream()
            .map(Role::getRoleId)
            .toList();
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds)
            ).stream()
            .map(RolePermission::getPermissionId)
            .distinct()
            .toList();
        if (CollectionUtils.isEmpty(permissionIds)) {
            return List.of();
        }
        return permissionMapper.selectBatchIds(permissionIds).stream()
            .map(Permission::getPermissionCode)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RoleVO> assignRoles(Long userId, List<Long> roleIds) {
        // 这是“用新列表整体替换旧角色”的语义，所以先清旧关联，再校验并插入新关联。
        requireUser(userId);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        List<Long> normalizedRoleIds = normalizeIds("roleIds", roleIds);
        if (CollectionUtils.isEmpty(normalizedRoleIds)) {
            return List.of();
        }
        ensureRolesExist(normalizedRoleIds);
        normalizedRoleIds.stream()
            .map(roleId -> buildUserRole(userId, roleId))
            .forEach(userRoleMapper::insert);
        return listRoles(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RoleVO> removeRole(Long userId, Long roleId) {
        requireUser(userId);
        if (roleId == null || roleId < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "roleId is invalid");
        }
        ensureRolesExist(List.of(roleId));
        userRoleMapper.delete(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, roleId)
        );
        return listRoles(userId);
    }

    private UserVO toUserVO(User user) {
        UserVO userVO = UserVO.from(user);
        if (userVO == null || user.getDepartmentId() == null) {
            return userVO;
        }
        Department department = departmentMapper.selectById(user.getDepartmentId());
        if (department != null) {
            userVO.setDepartmentName(department.getDepartmentName());
        }
        return userVO;
    }

    private List<RoleVO> listRoles(Long userId) {
        List<Role> roles = listEnabledRoles(userId);
        if (CollectionUtils.isEmpty(roles)) {
            return List.of();
        }
        return roles.stream()
            .map(RoleVO::from)
            .toList();
    }

    private List<Long> listRoleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)).stream()
            .map(UserRole::getRoleId)
            .distinct()
            .toList();
    }

    private List<Role> listEnabledRoles(Long userId) {
        List<Long> roleIds = listRoleIds(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return roleMapper.selectList(
            new LambdaQueryWrapper<Role>()
                .in(Role::getRoleId, roleIds)
                .eq(Role::getStatus, 1)
        );
    }

    private UserRole buildUserRole(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "userId is required");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        return user;
    }

    private void ensureRolesExist(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        List<Long> foundIds = roleMapper.selectBatchIds(roleIds).stream()
            .map(Role::getRoleId)
            .toList();
        List<Long> missingIds = roleIds.stream()
            .filter(roleId -> !foundIds.contains(roleId))
            .toList();
        if (!missingIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "roles not found: " + missingIds);
        }
    }

    private void bindDefaultRole(Long userId) {
        Role role = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, SecurityBootstrapCatalog.ROLE_USER).last("limit 1")
        );
        if (role == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "default role is not initialized");
        }
        userRoleMapper.insert(buildUserRole(userId, role.getRoleId()));
    }

    private void ensureDepartmentExists(Long departmentId) {
        Department department = departmentMapper.selectById(departmentId);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "department not found");
        }
    }

    private List<Long> normalizeIds(String fieldName, List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        if (ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " contains invalid id");
        }
        return ids.stream()
            .distinct()
            .toList();
    }

    private void ensureUniqueUsername(String username, Long excludeUserId) {
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .ne(excludeUserId != null, User::getUserId, excludeUserId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "username already exists");
        }
    }

    private void ensureUniqueEmail(String email, Long excludeUserId) {
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .ne(User::getUserId, excludeUserId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "email already exists");
        }
    }

    private void ensureUniquePhone(String phone, Long excludeUserId) {
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .ne(User::getUserId, excludeUserId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "phone already exists");
        }
    }

    private void requireText(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + " cannot be blank");
        }
    }

    private boolean isValidPassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            return false;
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }
}
