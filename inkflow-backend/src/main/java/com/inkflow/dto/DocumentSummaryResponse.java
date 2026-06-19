package com.inkflow.dto;

import com.inkflow.entity.Document;

import java.time.Instant;
import java.util.UUID;

public record DocumentSummaryResponse(UUID id, String title, UUID ownerId, Instant updatedAt) {

    public static DocumentSummaryResponse from(Document document) {
        return new DocumentSummaryResponse(
                document.getId(),
                document.getTitle(),
                document.getOwnerId(),
                document.getUpdatedAt()
        );
    }
}
