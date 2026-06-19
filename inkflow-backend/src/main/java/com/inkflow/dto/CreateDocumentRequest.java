package com.inkflow.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDocumentRequest(@NotBlank String title) {
}
