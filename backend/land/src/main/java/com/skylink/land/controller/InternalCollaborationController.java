package com.skylink.land.controller;

import com.skylink.land.dto.document.DocumentDto;
import com.skylink.land.service.document.DocumentCollaborationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/collaboration")
public class InternalCollaborationController {
    private final DocumentCollaborationService service;
    public InternalCollaborationController(DocumentCollaborationService service) { this.service = service; }

    @PostMapping("/authorize")
    public DocumentDto.CollaborationAuthorizationResponse authorize(
        @RequestHeader("X-SkyLink-Service-Token") String serviceToken,
        @RequestBody DocumentDto.CollaborationAuthorizationRequest request
    ) {
        return service.reauthorize(serviceToken, request.getUserId(), request.getDocumentId());
    }
}
