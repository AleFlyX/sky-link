package com.skylink.land.service.document;

import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.document.DocumentDto;
import com.skylink.land.entity.document.Document;

public interface DocumentService {
    DocumentDto.DocumentDetailResponse createDocument(Long userId, DocumentDto.CreateDocumentRequest request);
    PageResponse<DocumentDto.DocumentSummaryResponse> listDocuments(Long userId, DocumentDto.DocumentQueryRequest request);
    DocumentDto.DocumentDetailResponse getDocument(Long userId, Long documentId);
    DocumentDto.DocumentDetailResponse updateDocument(Long userId, Long documentId, DocumentDto.UpdateDocumentRequest request);
    void deleteDocument(Long userId, Long documentId);
    DocumentDto.DocumentPermissionResponse setUserPermission(Long userId, Long documentId, Long targetUserId, String permission);
    DocumentDto.DocumentGroupPermissionResponse setGroupPermission(Long userId, Long documentId, Long groupId, String permission);
    DocumentDto.DocumentPermissionListResponse listPermissions(Long userId, Long documentId);
    void removeUserPermission(Long userId, Long documentId, Long targetUserId);
    void removeGroupPermission(Long userId, Long documentId, Long groupId);
    String resolvePermission(Long userId, Document document);
}
