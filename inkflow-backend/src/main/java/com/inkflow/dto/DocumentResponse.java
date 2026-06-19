package com.inkflow.dto;

import com.inkflow.entity.Document;

import java.util.UUID;

public record DocumentResponse(UUID id, String title, String content, long version) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getContent(),
                document.getVersion()
        );
    }
}
