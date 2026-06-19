package com.inkflow.repository;

import com.inkflow.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}
