package com.skylink.land.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.skylink.land.dto.document.DocumentDto;
import com.skylink.land.entity.document.Document;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.mapper.document.DocumentGroupPermissionMapper;
import com.skylink.land.mapper.document.DocumentMapper;
import com.skylink.land.mapper.document.DocumentPermissionMapper;
import com.skylink.land.mapper.chat.ChatGroupMapper;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.service.document.impl.DocumentServiceImpl;
import com.skylink.land.service.identity.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTests {

    @Mock DocumentMapper documentMapper;
    @Mock DocumentPermissionMapper permissionMapper;
    @Mock DocumentGroupPermissionMapper groupPermissionMapper;
    @Mock UserMapper userMapper;
    @Mock UserService userService;
    @Mock ChatGroupMapper chatGroupMapper;

    private DocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocumentServiceImpl(
            documentMapper,
            permissionMapper,
            groupPermissionMapper,
            userMapper,
            userService,
            chatGroupMapper
        );
    }

    @Test
    void directlyAuthorizedEditorCanReadDocument() {
        Document document = document(10L, 1L, 1);
        when(documentMapper.selectById(10L)).thenReturn(document);
        when(permissionMapper.selectEffectivePermission(10L, 2L)).thenReturn(3);

        DocumentDto.DocumentDetailResponse response = service.getDocument(2L, 10L);

        assertThat(response.getDocumentId()).isEqualTo(10L);
        assertThat(response.getPermission()).isEqualTo("edit");
    }

    @Test
    void archivedDocumentCapsEditorAtRead() {
        Document document = document(10L, 1L, 3);
        when(permissionMapper.selectEffectivePermission(10L, 2L)).thenReturn(4);

        assertThat(service.resolvePermission(2L, document)).isEqualTo("read");
    }

    @Test
    void unauthorizedUserCannotLearnDocumentContent() {
        Document document = document(10L, 1L, 1);
        when(documentMapper.selectById(10L)).thenReturn(document);
        when(permissionMapper.selectEffectivePermission(10L, 2L)).thenReturn(null);
        when(userService.listRoleCodes(2L)).thenReturn(List.of("ROLE_USER"));

        assertThatThrownBy(() -> service.getDocument(2L, 10L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("document not found");
    }

    @Test
    void creatorAlwaysReceivesManagePermission() {
        assertThat(service.resolvePermission(1L, document(10L, 1L, 1))).isEqualTo("manage");
    }


    private Document document(Long id, Long creatorId, int status) {
        Document document = new Document();
        document.setDocumentId(id);
        document.setCreatorId(creatorId);
        document.setTitle("Project plan");
        document.setContent("# Plan");
        document.setStatus(status);
        return document;
    }
}
