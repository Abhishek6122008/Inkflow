package com.inkflow.dto;

import com.inkflow.entity.User;
import com.inkflow.enums.DocumentRole;

import java.util.UUID;

public record CollaboratorResponse(UUID userId, String email, String displayName, DocumentRole role) {

    public static CollaboratorResponse from(User user, DocumentRole role) {
        return new CollaboratorResponse(user.getId(), user.getEmail(), user.getDisplayName(), role);
    }
}
