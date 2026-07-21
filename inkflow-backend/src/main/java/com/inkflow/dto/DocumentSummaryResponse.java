package com.inkflow.dto;

import com.inkflow.entity.Document;
import com.inkflow.enums.DocumentRole;

import java.time.Instant;
import java.util.UUID;

public record DocumentSummaryResponse(UUID id, String title, UUID ownerId, Instant updatedAt, DocumentRole role) {

    public static DocumentSummaryResponse from(Document document, DocumentRole role) {
        return new DocumentSummaryResponse(
                document.getId(),
                document.getTitle(),
                document.getOwnerId(),
                document.getUpdatedAt(),
                role
        );
    }
}
