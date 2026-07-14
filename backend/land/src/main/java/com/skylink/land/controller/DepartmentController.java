package com.skylink.land.controller;

import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.department.DepartmentDto;
import com.skylink.land.dto.user.UserDto;
import com.skylink.land.service.identity.DepartmentService;
import com.skylink.land.vo.identity.DepartmentVO;
import com.skylink.land.vo.identity.UserVO;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<DepartmentDto.DepartmentTreeResponse> listDepartments() {
        return departmentService.listDepartmentVO().stream()
            .map(this::toDepartmentTreeResponse)
            .toList();
    }

    @PostMapping
    public DepartmentDto.DepartmentTreeResponse createDepartment(@RequestBody DepartmentDto.SaveDepartmentRequest request) {
        return toDepartmentTreeResponse(departmentService.createDepartment(request));
    }

    @PutMapping("/{departmentId}")
    public DepartmentDto.DepartmentTreeResponse updateDepartment(
        @PathVariable Long departmentId,
        @RequestBody DepartmentDto.SaveDepartmentRequest request
    ) {
        return toDepartmentTreeResponse(departmentService.updateDepartment(departmentId, request));
    }

    @DeleteMapping("/{departmentId}")
    public void deleteDepartment(@PathVariable Long departmentId) {
        departmentService.deleteDepartment(departmentId);
    }

    @GetMapping("/{departmentId}/members")
    public PageResponse<UserDto.UserSummaryResponse> pageDepartmentMembers(
        @PathVariable Long departmentId,
        DepartmentDto.DepartmentMemberQueryRequest request
    ) {
        PageResponse<UserVO> page = departmentService.pageDepartmentMembers(departmentId, request);
        return PageResponse.<UserDto.UserSummaryResponse>builder()
            .total(page.getTotal())
            .page(page.getPage())
            .size(page.getSize())
            .records(page.getRecords().stream().map(this::toUserSummaryResponse).toList())
            .build();
    }

    private DepartmentDto.DepartmentTreeResponse toDepartmentTreeResponse(DepartmentVO department) {
        return DepartmentDto.DepartmentTreeResponse.builder()
            .departmentId(department.getDepartmentId())
            .departmentName(department.getDepartmentName())
            .leaderId(department.getLeaderId())
            .leaderName(department.getLeaderName())
            .description(department.getDescription())
            .parentId(null)
            .memberCount(department.getMemberCount())
            .children(department.getChildren() == null
                ? List.of()
                : department.getChildren().stream().map(this::toDepartmentTreeResponse).toList())
            .build();
    }

    private UserDto.UserSummaryResponse toUserSummaryResponse(UserVO user) {
        return UserDto.UserSummaryResponse.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .avatar(user.getAvatar())
            .email(user.getEmail())
            .phone(user.getPhone())
            .status(user.getStatus())
            .departmentId(user.getDepartmentId())
            .departmentName(user.getDepartmentName())
            .createTime(user.getCreateTime())
            .build();
    }
}
