package com.inkflow.service;

import com.inkflow.dto.CreateDocumentRequest;
import com.inkflow.dto.DocumentResponse;
import com.inkflow.dto.DocumentSummaryResponse;
import com.inkflow.dto.UpdateDocumentContentRequest;
import com.inkflow.entity.Document;
import com.inkflow.entity.User;
import com.inkflow.exception.DocumentNotFoundException;
import com.inkflow.repository.DocumentRepository;
import com.inkflow.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final String EMPTY_DELTA = "[{\"insert\":\"\\n\"}]";

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public List<DocumentSummaryResponse> listForUser(String email) {
        UUID ownerId = resolveUserId(email);
        return documentRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId).stream()
                .map(DocumentSummaryResponse::from)
                .toList();
    }

    public DocumentSummaryResponse create(String email, CreateDocumentRequest request) {
        UUID ownerId = resolveUserId(email);

        Document document = new Document();
        document.setTitle(request.title());
        document.setOwnerId(ownerId);
        document.setContent(EMPTY_DELTA);
        document.setVersion(0);

        return DocumentSummaryResponse.from(documentRepository.save(document));
    }

    public DocumentResponse get(String email, UUID documentId) {
        Document document = findOwnedDocument(email, documentId);
        return DocumentResponse.from(document);
    }

    public DocumentResponse updateContent(String email, UUID documentId, UpdateDocumentContentRequest request) {
        Document document = findOwnedDocument(email, documentId);
        document.setContent(request.content());
        document.setVersion(document.getVersion() + 1);
        return DocumentResponse.from(documentRepository.save(document));
    }

    private Document findOwnedDocument(String email, UUID documentId) {
        UUID ownerId = resolveUserId(email);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!document.getOwnerId().equals(ownerId)) {
            throw new DocumentNotFoundException(documentId);
        }

        return document;
    }

    private UUID resolveUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("No user with email " + email));
        return user.getId();
    }
}
