package com.inkflow.service;

import com.inkflow.dto.AddCollaboratorRequest;
import com.inkflow.dto.CollaboratorResponse;
import com.inkflow.dto.CreateDocumentRequest;
import com.inkflow.dto.DocumentResponse;
import com.inkflow.dto.DocumentSummaryResponse;
import com.inkflow.dto.RenameDocumentRequest;
import com.inkflow.dto.UpdateDocumentContentRequest;
import com.inkflow.entity.Document;
import com.inkflow.entity.DocumentCollaborator;
import com.inkflow.entity.User;
import com.inkflow.enums.DocumentRole;
import com.inkflow.exception.DocumentAccessDeniedException;
import com.inkflow.exception.DocumentNotFoundException;
import com.inkflow.repository.DocumentCollaboratorRepository;
import com.inkflow.repository.DocumentRepository;
import com.inkflow.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final String EMPTY_DELTA = "[{\"insert\":\"\\n\"}]";

    private final DocumentRepository documentRepository;
    private final DocumentCollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;

    public List<DocumentSummaryResponse> listForUser(String email) {
        UUID userId = resolveUserId(email);
        List<DocumentCollaborator> memberships = collaboratorRepository.findByUserId(userId);

        Map<UUID, DocumentRole> roleByDocumentId = memberships.stream()
                .collect(Collectors.toMap(
                        DocumentCollaborator::getDocumentId,
                        DocumentCollaborator::getRole
                ));

        return documentRepository.findAllById(roleByDocumentId.keySet()).stream()
                .sorted(Comparator.comparing(Document::getUpdatedAt).reversed())
                .map(document -> DocumentSummaryResponse.from(document, roleByDocumentId.get(document.getId())))
                .toList();
    }

    public DocumentSummaryResponse create(String email, CreateDocumentRequest request) {
        UUID ownerId = resolveUserId(email);

        Document document = new Document();
        document.setTitle(request.title());
        document.setOwnerId(ownerId);
        document.setContent(EMPTY_DELTA);
        document.setVersion(0);
        document = documentRepository.save(document);

        DocumentCollaborator ownerMembership = new DocumentCollaborator();
        ownerMembership.setDocumentId(document.getId());
        ownerMembership.setUserId(ownerId);
        ownerMembership.setRole(DocumentRole.OWNER);
        collaboratorRepository.save(ownerMembership);

        return DocumentSummaryResponse.from(document, DocumentRole.OWNER);
    }

    public DocumentResponse get(String email, UUID documentId) {
        Access access = resolveAccess(email, documentId);
        return DocumentResponse.from(access.document(), access.role());
    }

    public DocumentResponse updateContent(String email, UUID documentId, UpdateDocumentContentRequest request) {
        Access access = resolveAccess(email, documentId);
        requireEditAccess(access.role(), documentId);

        Document document = access.document();
        document.setContent(request.content());
        document.setVersion(document.getVersion() + 1);
        return DocumentResponse.from(documentRepository.save(document), access.role());
    }

    public DocumentSummaryResponse rename(String email, UUID documentId, RenameDocumentRequest request) {
        Access access = resolveAccess(email, documentId);
        requireOwner(access.role(), documentId);

        Document document = access.document();
        document.setTitle(request.title());
        return DocumentSummaryResponse.from(documentRepository.save(document), access.role());
    }

    public void delete(String email, UUID documentId) {
        Access access = resolveAccess(email, documentId);
        requireOwner(access.role(), documentId);
        documentRepository.delete(access.document());
    }

    public List<CollaboratorResponse> addCollaborator(String email, UUID documentId, AddCollaboratorRequest request) {
        Access access = resolveAccess(email, documentId);
        requireOwner(access.role(), documentId);

        if (request.role() == DocumentRole.OWNER) {
            throw new DocumentAccessDeniedException("Cannot grant the OWNER role to a collaborator");
        }

        User collaboratorUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException("No user with email " + request.email()));

        DocumentCollaborator membership = collaboratorRepository
                .findByDocumentIdAndUserId(documentId, collaboratorUser.getId())
                .orElseGet(DocumentCollaborator::new);
        membership.setDocumentId(documentId);
        membership.setUserId(collaboratorUser.getId());
        membership.setRole(request.role());
        collaboratorRepository.save(membership);

        return collaboratorRepository.findByDocumentId(documentId).stream()
                .map(c -> CollaboratorResponse.from(userRepository.findById(c.getUserId())
                        .orElseThrow(() -> new EntityNotFoundException("No user with id " + c.getUserId())), c.getRole()))
                .toList();
    }

    /**
     * Resolves the caller's membership for a document and verifies they may
     * submit edits (OWNER/EDITOR), for use by the WebSocket edit pipeline.
     * Throws {@link DocumentNotFoundException} if not a collaborator,
     * {@link DocumentAccessDeniedException} if VIEWER.
     */
    public UUID resolveEditorUserId(String email, UUID documentId) {
        UUID userId = resolveUserId(email);
        DocumentCollaborator membership = collaboratorRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        requireEditAccess(membership.getRole(), documentId);
        return userId;
    }

    private Access resolveAccess(String email, UUID documentId) {
        UUID userId = resolveUserId(email);
        DocumentCollaborator membership = collaboratorRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        return new Access(document, membership.getRole());
    }

    private void requireEditAccess(DocumentRole role, UUID documentId) {
        if (role == DocumentRole.VIEWER) {
            throw new DocumentAccessDeniedException("Viewers cannot edit document " + documentId);
        }
    }

    private void requireOwner(DocumentRole role, UUID documentId) {
        if (role != DocumentRole.OWNER) {
            throw new DocumentAccessDeniedException("Only the owner can perform this action on document " + documentId);
        }
    }

    private UUID resolveUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("No user with email " + email));
        return user.getId();
    }

    private record Access(Document document, DocumentRole role) {
    }
}
