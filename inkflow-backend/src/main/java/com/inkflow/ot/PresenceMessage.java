package com.inkflow.ot;

import java.util.List;
import java.util.UUID;

public record PresenceMessage(UUID documentId, List<PresenceEntry> users) {
}
