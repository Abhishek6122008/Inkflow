package com.inkflow.controller;

import com.inkflow.dto.CreateDocumentRequest;
import com.inkflow.dto.DocumentResponse;
import com.inkflow.dto.DocumentSummaryResponse;
import com.inkflow.dto.UpdateDocumentContentRequest;
import com.inkflow.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<List<DocumentSummaryResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(documentService.listForUser(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<DocumentSummaryResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateDocumentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.create(authentication.getName(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> get(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.get(authentication.getName(), id));
    }

    @PutMapping("/{id}/content")
    public ResponseEntity<DocumentResponse> updateContent(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDocumentContentRequest request
    ) {
        return ResponseEntity.ok(documentService.updateContent(authentication.getName(), id, request));
    }
}
