package com.inkflow.dto;

public record AuthResponse(String accessToken, long expiresIn, UserResponse user) {
}
