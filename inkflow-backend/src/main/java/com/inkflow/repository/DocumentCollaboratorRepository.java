package com.inkflow.repository;

import com.inkflow.entity.DocumentCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentCollaboratorRepository extends JpaRepository<DocumentCollaborator, UUID> {

    Optional<DocumentCollaborator> findByDocumentIdAndUserId(UUID documentId, UUID userId);

    List<DocumentCollaborator> findByUserId(UUID userId);

    List<DocumentCollaborator> findByDocumentId(UUID documentId);

    boolean existsByDocumentIdAndUserId(UUID documentId, UUID userId);
}
