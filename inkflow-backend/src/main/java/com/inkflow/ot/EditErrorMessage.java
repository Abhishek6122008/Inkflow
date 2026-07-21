package com.inkflow.ot;

import java.util.UUID;

/** Sent to the offending client only (via STOMP user destination) when an edit is rejected. */
public record EditErrorMessage(UUID documentId, String code, String message) {
}
