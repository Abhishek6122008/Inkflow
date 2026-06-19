package com.inkflow.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateDocumentContentRequest(@NotNull String content) {
}
