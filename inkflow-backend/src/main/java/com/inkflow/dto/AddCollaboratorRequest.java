package com.inkflow.dto;

import com.inkflow.enums.DocumentRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddCollaboratorRequest(@NotBlank String email, @NotNull DocumentRole role) {
}
