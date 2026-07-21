package com.skylink.land.controller;

import com.skylink.land.auth.AuthContext;
import com.skylink.land.auth.RequirePermission;
import com.skylink.land.dto.common.ApiResponse;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.document.DocumentDto;
import com.skylink.land.service.document.DocumentService;
import com.skylink.land.service.document.DocumentCollaborationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    private final DocumentService service;
    private final DocumentCollaborationService collaborationService;
    public DocumentController(DocumentService service, DocumentCollaborationService collaborationService) {
        this.service = service; this.collaborationService = collaborationService;
    }

    @PostMapping @RequirePermission("document:create")
    public DocumentDto.DocumentDetailResponse create(@RequestBody DocumentDto.CreateDocumentRequest request) {
        return service.createDocument(AuthContext.requireUserId(), request);
    }
    @GetMapping @RequirePermission("document:list")
    public PageResponse<DocumentDto.DocumentSummaryResponse> list(DocumentDto.DocumentQueryRequest request) {
        return service.listDocuments(AuthContext.requireUserId(), request);
    }
    @GetMapping("/{documentId}") @RequirePermission("document:get")
    public DocumentDto.DocumentDetailResponse get(@PathVariable Long documentId) {
        return service.getDocument(AuthContext.requireUserId(), documentId);
    }
    @PostMapping("/{documentId}/collaboration-ticket") @RequirePermission("document:get")
    public DocumentDto.CollaborationTicketResponse ticket(@PathVariable Long documentId) {
        // 先通过普通 JWT + document:get 功能权限，再由 Service 检查这篇具体文档的资源级权限。
        // 返回的是短期协同票据和 BFF WebSocket 地址，不是把用户的长期 access token 交给协同服务。
        return collaborationService.issueTicket(AuthContext.requireUserId(), documentId);
    }
    @PutMapping("/{documentId}") @RequirePermission("document:update")
    public DocumentDto.DocumentDetailResponse update(@PathVariable Long documentId, @RequestBody DocumentDto.UpdateDocumentRequest request) {
        return service.updateDocument(AuthContext.requireUserId(), documentId, request);
    }
    @DeleteMapping("/{documentId}") @RequirePermission("document:delete")
    public ApiResponse<Void> delete(@PathVariable Long documentId) {
        service.deleteDocument(AuthContext.requireUserId(), documentId); return ApiResponse.success("document deleted", null);
    }
    @PutMapping("/{documentId}/permissions/{userId}") @RequirePermission("document:permission:user:set")
    public DocumentDto.DocumentPermissionResponse setUser(@PathVariable Long documentId, @PathVariable Long userId, @RequestBody DocumentDto.GrantDocumentPermissionRequest request) {
        return service.setUserPermission(AuthContext.requireUserId(), documentId, userId, request.getPermissionType());
    }
    @PutMapping("/{documentId}/group-permissions/{groupId}") @RequirePermission("document:permission:group:set")
    public DocumentDto.DocumentGroupPermissionResponse setGroup(@PathVariable Long documentId, @PathVariable Long groupId, @RequestBody DocumentDto.GrantDocumentGroupPermissionRequest request) {
        return service.setGroupPermission(AuthContext.requireUserId(), documentId, groupId, request.getPermissionType());
    }
    @GetMapping("/{documentId}/permissions") @RequirePermission("document:permission:list")
    public DocumentDto.DocumentPermissionListResponse permissions(@PathVariable Long documentId) {
        return service.listPermissions(AuthContext.requireUserId(), documentId);
    }
    @DeleteMapping("/{documentId}/permissions/{userId}") @RequirePermission("document:permission:user:delete")
    public ApiResponse<Void> removeUser(@PathVariable Long documentId, @PathVariable Long userId) {
        service.removeUserPermission(AuthContext.requireUserId(), documentId, userId); return ApiResponse.success("permission removed", null);
    }
    @DeleteMapping("/{documentId}/group-permissions/{groupId}") @RequirePermission("document:permission:group:delete")
    public ApiResponse<Void> removeGroup(@PathVariable Long documentId, @PathVariable Long groupId) {
        service.removeGroupPermission(AuthContext.requireUserId(), documentId, groupId); return ApiResponse.success("group permission removed", null);
    }
}
