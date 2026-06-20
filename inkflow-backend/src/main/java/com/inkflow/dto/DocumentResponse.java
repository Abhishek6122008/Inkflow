package com.inkflow.dto;

import com.inkflow.entity.Document;
import com.inkflow.enums.DocumentRole;

import java.util.UUID;

public record DocumentResponse(UUID id, String title, String content, long version, DocumentRole role) {

    public static DocumentResponse from(Document document, DocumentRole role) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getContent(),
                document.getVersion(),
                role
        );
    }
}
