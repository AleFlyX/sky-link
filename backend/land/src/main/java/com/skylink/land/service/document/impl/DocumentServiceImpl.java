package com.skylink.land.service.document.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.document.DocumentDto;
import com.skylink.land.dto.user.UserDto;
import com.skylink.land.entity.document.Document;
import com.skylink.land.entity.document.DocumentGroupPermission;
import com.skylink.land.entity.document.DocumentPermission;
import com.skylink.land.entity.identity.User;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.chat.ChatGroupMapper;
import com.skylink.land.mapper.document.DocumentGroupPermissionMapper;
import com.skylink.land.mapper.document.DocumentMapper;
import com.skylink.land.mapper.document.DocumentPermissionMapper;
import com.skylink.land.mapper.document.DocumentCollaborationStateMapper;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.service.document.DocumentService;
import com.skylink.land.service.identity.UserService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final int STATUS_PRIVATE = 1;
    private static final int STATUS_TEAM = 2;
    private static final int STATUS_ARCHIVED = 3;
    private static final int PERMISSION_READ = 1;
    private static final int PERMISSION_EDIT = 3;
    private static final int PERMISSION_MANAGE = 4;

    private final DocumentMapper documentMapper;
    private final DocumentPermissionMapper permissionMapper;
    private final DocumentGroupPermissionMapper groupPermissionMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final ChatGroupMapper chatGroupMapper;
    private final DocumentCollaborationStateMapper collaborationStateMapper;

    public DocumentServiceImpl(
        DocumentMapper documentMapper,
        DocumentPermissionMapper permissionMapper,
        DocumentGroupPermissionMapper groupPermissionMapper,
        UserMapper userMapper,
        UserService userService,
        ChatGroupMapper chatGroupMapper,
        DocumentCollaborationStateMapper collaborationStateMapper
    ) {
        this.documentMapper = documentMapper;
        this.permissionMapper = permissionMapper;
        this.groupPermissionMapper = groupPermissionMapper;
        this.userMapper = userMapper;
        this.userService = userService;
        this.chatGroupMapper = chatGroupMapper;
        this.collaborationStateMapper = collaborationStateMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDto.DocumentDetailResponse createDocument(Long userId, DocumentDto.CreateDocumentRequest request) {
        requireUser(userId);
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw badRequest("title is required");
        }
        validateTitle(request.getTitle());
        Document document = new Document();
        document.setTitle(request.getTitle().trim());
        document.setContent(request.getContent());
        document.setCreatorId(userId);
        document.setStatus(parseStatus(request.getStatus(), STATUS_PRIVATE));
        documentMapper.insert(document);
        return getDocument(userId, document.getDocumentId());
    }

    @Override
    public PageResponse<DocumentDto.DocumentSummaryResponse> listDocuments(Long userId, DocumentDto.DocumentQueryRequest request) {
        requireUser(userId);
        DocumentDto.DocumentQueryRequest query = request == null ? new DocumentDto.DocumentQueryRequest() : request;
        Page<Document> page = documentMapper.selectAccessiblePage(
            query.toMybatisPage(), userId, trimToNull(query.getTitle()), isAdministrator(userId)
        );
        Map<Long, User> creators = loadUsers(page.getRecords().stream().map(Document::getCreatorId).toList());
        IPage<DocumentDto.DocumentSummaryResponse> response = page.convert(document -> {
            User creator = creators.get(document.getCreatorId());
            return DocumentDto.DocumentSummaryResponse.builder()
                .documentId(document.getDocumentId())
                .title(document.getTitle())
                .status(statusName(document.getStatus()))
                .creatorId(document.getCreatorId())
                .creatorName(displayName(creator))
                .createTime(document.getCreateTime())
                .updateTime(document.getUpdateTime())
                .permission(resolvePermission(userId, document))
                .build();
        });
        return PageResponse.of(response);
    }

    @Override
    public DocumentDto.DocumentDetailResponse getDocument(Long userId, Long documentId) {
        Document document = requireDocument(documentId);
        String permission = resolvePermission(userId, document);
        if (permission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
        }
        User creator = userMapper.selectById(document.getCreatorId());
        return DocumentDto.DocumentDetailResponse.builder()
            .documentId(document.getDocumentId())
            .title(document.getTitle())
            .content(document.getContent())
            .status(statusName(document.getStatus()))
            .creatorId(document.getCreatorId())
            .creatorName(displayName(creator))
            .createTime(document.getCreateTime())
            .updateTime(document.getUpdateTime())
            .permission(permission)
            .collaborative(collaborationStateMapper.selectById(documentId) != null)
            .permissions(List.of())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDto.DocumentDetailResponse updateDocument(Long userId, Long documentId, DocumentDto.UpdateDocumentRequest request) {
        Document document = requireDocument(documentId);
        requireAtLeast(userId, document, PERMISSION_EDIT);
        if (document.getStatus() == STATUS_ARCHIVED && !canManage(userId, document)) {
            throw new BusinessException(ErrorCode.CONFLICT, "archived document is read-only");
        }
        if (request == null) throw badRequest("request body is required");
        if (request.getContent() != null && collaborationStateMapper.selectById(documentId) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "collaborative document content must be updated through Yjs");
        }
        if (request.getTitle() != null) {
            validateTitle(request.getTitle());
            document.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null) document.setContent(request.getContent());
        if (request.getStatus() != null) {
            if (!canManage(userId, document)) throw forbidden("manage permission is required to change status");
            document.setStatus(parseStatus(request.getStatus(), document.getStatus()));
        }
        documentMapper.updateById(document);
        return getDocument(userId, documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long userId, Long documentId) {
        Document document = requireDocument(documentId);
        if (!canManage(userId, document)) throw forbidden("manage permission is required");
        documentMapper.deleteById(documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDto.DocumentPermissionResponse setUserPermission(Long userId, Long documentId, Long targetUserId, String value) {
        Document document = requireDocument(documentId);
        if (!canManage(userId, document)) throw forbidden("manage permission is required");
        User target = userMapper.selectById(targetUserId);
        if (target == null) throw badRequest("target user does not exist");
        int level = parsePermission(value);
        DocumentPermission permission = new DocumentPermission();
        permission.setDocumentId(documentId);
        permission.setUserId(targetUserId);
        permission.setPermissionType(level);
        permissionMapper.upsert(permission);
        return toUserPermission(permission, target);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDto.DocumentGroupPermissionResponse setGroupPermission(Long userId, Long documentId, Long groupId, String value) {
        Document document = requireDocument(documentId);
        if (!canManage(userId, document)) throw forbidden("manage permission is required");
        if (chatGroupMapper.selectById(groupId) == null) throw badRequest("target group does not exist");
        DocumentGroupPermission permission = new DocumentGroupPermission();
        permission.setDocumentId(documentId);
        permission.setGroupId(groupId);
        permission.setPermissionType(parsePermission(value));
        groupPermissionMapper.upsert(permission);
        return toGroupPermission(permission);
    }

    @Override
    public DocumentDto.DocumentPermissionListResponse listPermissions(Long userId, Long documentId) {
        Document document = requireDocument(documentId);
        if (!canManage(userId, document)) throw forbidden("manage permission is required");
        List<DocumentPermission> userPermissions = permissionMapper.selectByDocumentId(documentId);
        Map<Long, User> users = loadUsers(userPermissions.stream().map(DocumentPermission::getUserId).toList());
        return DocumentDto.DocumentPermissionListResponse.builder()
            .users(userPermissions.stream().map(p -> toUserPermission(p, users.get(p.getUserId()))).toList())
            .groups(groupPermissionMapper.selectByDocumentId(documentId).stream().map(this::toGroupPermission).toList())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserPermission(Long userId, Long documentId, Long targetUserId) {
        Document document = requireDocument(documentId);
        if (!canManage(userId, document)) throw forbidden("manage permission is required");
        permissionMapper.deleteGrant(documentId, targetUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeGroupPermission(Long userId, Long documentId, Long groupId) {
        Document document = requireDocument(documentId);
        if (!canManage(userId, document)) throw forbidden("manage permission is required");
        groupPermissionMapper.deleteGrant(documentId, groupId);
    }

    @Override
    public String resolvePermission(Long userId, Document document) {
        requireUser(userId);
        int level;
        if (Objects.equals(document.getCreatorId(), userId) || isAdministrator(userId)) level = PERMISSION_MANAGE;
        else {
            Integer granted = permissionMapper.selectEffectivePermission(document.getDocumentId(), userId);
            if (granted == null) return null;
            level = granted;
        }
        if (document.getStatus() == STATUS_ARCHIVED) level = PERMISSION_READ;
        return permissionName(level);
    }

    private void requireAtLeast(Long userId, Document document, int required) {
        String permission = resolvePermission(userId, document);
        if (permission == null || parsePermission(permission) < required) throw forbidden("document permission is insufficient");
    }

    private boolean canManage(Long userId, Document document) {
        return Objects.equals(document.getCreatorId(), userId) || isAdministrator(userId)
            || "manage".equals(resolvePermission(userId, document));
    }

    private boolean isAdministrator(Long userId) {
        List<String> roles = userService.listRoleCodes(userId);
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPER_ADMIN"));
    }

    private Document requireDocument(Long documentId) {
        if (documentId == null || documentId < 1) throw badRequest("documentId is invalid");
        Document document = documentMapper.selectById(documentId);
        if (document == null) throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
        return document;
    }

    private void requireUser(Long userId) {
        if (userId == null || userId < 1) throw new BusinessException(ErrorCode.UNAUTHORIZED, "current user is missing");
    }

    private int parsePermission(String value) {
        if (!StringUtils.hasText(value)) throw badRequest("permissionType is required");
        return switch (value.trim().toLowerCase()) {
            case "read" -> PERMISSION_READ;
            case "edit" -> PERMISSION_EDIT;
            case "manage" -> PERMISSION_MANAGE;
            default -> throw badRequest("permissionType must be read, edit or manage");
        };
    }

    private String permissionName(int level) {
        if (level >= PERMISSION_MANAGE) return "manage";
        if (level >= PERMISSION_EDIT) return "edit";
        return "read";
    }

    private int parseStatus(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) return defaultValue;
        return switch (value.trim().toLowerCase()) {
            case "private" -> STATUS_PRIVATE;
            case "team" -> STATUS_TEAM;
            case "archived" -> STATUS_ARCHIVED;
            default -> throw badRequest("status must be private, team or archived");
        };
    }

    private String statusName(Integer status) {
        return switch (status == null ? STATUS_PRIVATE : status) {
            case STATUS_TEAM -> "team";
            case STATUS_ARCHIVED -> "archived";
            default -> "private";
        };
    }

    private void validateTitle(String title) {
        if (!StringUtils.hasText(title)) throw badRequest("title is required");
        if (title.trim().length() > 100) throw badRequest("title must not exceed 100 characters");
    }

    private Map<Long, User> loadUsers(List<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(User::getUserId, Function.identity()));
    }

    private String displayName(User user) {
        if (user == null) return null;
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private DocumentDto.DocumentPermissionResponse toUserPermission(DocumentPermission p, User user) {
        return DocumentDto.DocumentPermissionResponse.builder()
            .documentId(p.getDocumentId()).userId(p.getUserId()).permissionType(permissionName(p.getPermissionType()))
            .createTime(p.getCreateTime()).user(toUserSummary(user)).build();
    }

    private DocumentDto.DocumentGroupPermissionResponse toGroupPermission(DocumentGroupPermission p) {
        return DocumentDto.DocumentGroupPermissionResponse.builder()
            .documentId(p.getDocumentId()).groupId(p.getGroupId()).permissionType(permissionName(p.getPermissionType()))
            .createTime(p.getCreateTime()).build();
    }

    private UserDto.UserSummaryResponse toUserSummary(User user) {
        if (user == null) return null;
        return UserDto.UserSummaryResponse.builder().userId(user.getUserId()).username(user.getUsername())
            .nickname(user.getNickname()).email(user.getEmail()).phone(user.getPhone()).status(user.getStatus())
            .departmentId(user.getDepartmentId()).createTime(user.getCreateTime()).build();
    }

    private String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private BusinessException badRequest(String message) { return new BusinessException(ErrorCode.BAD_REQUEST, message); }
    private BusinessException forbidden(String message) { return new BusinessException(ErrorCode.FORBIDDEN, message); }
}
