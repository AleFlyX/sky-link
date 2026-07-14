package com.skylink.land.service.document;

import com.skylink.land.collaboration.CollaborationProperties;
import com.skylink.land.collaboration.CollaborationTicket;
import com.skylink.land.collaboration.CollaborationTicketProvider;
import com.skylink.land.dto.document.DocumentDto;
import com.skylink.land.entity.document.Document;
import com.skylink.land.entity.identity.User;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.document.DocumentMapper;
import com.skylink.land.mapper.identity.UserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DocumentCollaborationService {
    private final DocumentService documentService;
    private final DocumentMapper documentMapper;
    private final UserMapper userMapper;
    private final CollaborationTicketProvider ticketProvider;
    private final CollaborationProperties properties;

    public DocumentCollaborationService(DocumentService documentService, DocumentMapper documentMapper, UserMapper userMapper,
                                        CollaborationTicketProvider ticketProvider, CollaborationProperties properties) {
        this.documentService = documentService; this.documentMapper = documentMapper; this.userMapper = userMapper;
        this.ticketProvider = ticketProvider; this.properties = properties;
    }

    public DocumentDto.CollaborationTicketResponse issueTicket(Long userId, Long documentId) {
        Authorization authorization = authorize(userId, documentId);
        if (!authorization.allowed()) throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
        CollaborationTicket ticket = ticketProvider.issue(userId, documentId, authorization.permission(), authorization.displayName());
        return DocumentDto.CollaborationTicketResponse.builder().token(ticket.token())
            .websocketUrl(properties.getWebsocketUrl()).expiresAt(ticket.expiresAt())
            .permission(authorization.permission()).build();
    }

    public DocumentDto.CollaborationAuthorizationResponse reauthorize(String serviceToken, Long userId, Long documentId) {
        requireServiceToken(serviceToken);
        Authorization authorization = authorize(userId, documentId);
        return DocumentDto.CollaborationAuthorizationResponse.builder().allowed(authorization.allowed())
            .permission(authorization.permission()).displayName(authorization.displayName())
            .documentStatus(authorization.status()).build();
    }

    private Authorization authorize(Long userId, Long documentId) {
        if (userId == null || documentId == null) return Authorization.denied();
        User user = userMapper.selectById(userId);
        Document document = documentMapper.selectById(documentId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1 || document == null) return Authorization.denied();
        String permission = documentService.resolvePermission(userId, document);
        if (permission == null) return Authorization.denied();
        String name = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
        String status = document.getStatus() == 3 ? "archived" : document.getStatus() == 2 ? "team" : "private";
        return new Authorization(true, permission, name, status);
    }

    private void requireServiceToken(String supplied) {
        String expected = properties.getServiceToken();
        if (!StringUtils.hasText(expected) || expected.length() < 32) throw new IllegalStateException("collaboration service token is not configured");
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = String.valueOf(supplied).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(a, b)) throw new BusinessException(ErrorCode.FORBIDDEN, "invalid collaboration service credential");
    }

    private record Authorization(boolean allowed, String permission, String displayName, String status) {
        static Authorization denied() { return new Authorization(false, null, null, null); }
    }
}
