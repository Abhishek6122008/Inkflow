package com.inkflow.ot;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server -> clients payload broadcast on {@code /topic/doc/{id}}. */
public record BroadcastMessage(UUID documentId, List<Map<String, Object>> op, long version, UUID authorId) {
}
