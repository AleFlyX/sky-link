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
        // 取票前重新计算用户对该文档的实时权限；前端不能靠旧的页面状态自行声明 edit/manage。
        Authorization authorization = authorize(userId, documentId);
        if (!authorization.allowed()) throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
        // 票据绑定用户、文档、权限和显示名，BFF 在 WebSocket 握手时会验签并核对文档 ID。
        CollaborationTicket ticket = ticketProvider.issue(userId, documentId, authorization.permission(), authorization.displayName());
        return DocumentDto.CollaborationTicketResponse.builder().token(ticket.token())
            .websocketUrl(properties.getWebsocketUrl()).expiresAt(ticket.expiresAt())
            .permission(authorization.permission()).build();
    }

    public DocumentDto.CollaborationAuthorizationResponse reauthorize(String serviceToken, Long userId, Long documentId) {
        // 该接口只接受协同 BFF 的内部服务令牌，浏览器用户的 JWT 不能直接调用它。
        requireServiceToken(serviceToken);
        // 长连接期间可撤销/降级权限，因此 BFF 会周期性调用这里获得最新结果。
        Authorization authorization = authorize(userId, documentId);
        return DocumentDto.CollaborationAuthorizationResponse.builder().allowed(authorization.allowed())
            .permission(authorization.permission()).displayName(authorization.displayName())
            .documentStatus(authorization.status()).build();
    }

    private Authorization authorize(Long userId, Long documentId) {
        // 任何一个 ID 缺失时直接拒绝，避免下方数据库查询产生模糊的空值语义。
        if (userId == null || documentId == null) return Authorization.denied();
        User user = userMapper.selectById(userId);
        Document document = documentMapper.selectById(documentId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1 || document == null) return Authorization.denied();
        // 复用文档业务层的唯一权限规则，避免 HTTP 文档接口和协同 WebSocket 出现两套不一致授权。
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
        // 常量时间比较内部服务令牌，避免发现首个不同字节就提前返回的时序泄漏。
        if (!MessageDigest.isEqual(a, b)) throw new BusinessException(ErrorCode.FORBIDDEN, "invalid collaboration service credential");
    }

    private record Authorization(boolean allowed, String permission, String displayName, String status) {
        static Authorization denied() { return new Authorization(false, null, null, null); }
    }
}
