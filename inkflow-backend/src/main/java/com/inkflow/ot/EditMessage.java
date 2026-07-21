package com.inkflow.ot;

import java.util.List;
import java.util.Map;

/** Client -> server payload sent to {@code /app/doc/{id}/edit}. */
public record EditMessage(List<Map<String, Object>> op, long baseVersion) {
}
