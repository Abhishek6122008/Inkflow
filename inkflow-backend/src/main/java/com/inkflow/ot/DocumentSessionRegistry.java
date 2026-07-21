package com.inkflow.ot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkflow.entity.Document;
import com.inkflow.exception.DocumentNotFoundException;
import com.inkflow.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the single in-memory {@link DocumentSession} per open document.
 * Sessions are created lazily on first edit and persisted to Postgres after
 * every applied op (simplest correct cadence for demo scale — see
 * docs/architecture.md "Open decisions"; debounced/snapshot persistence can
 * replace this later without changing the OT logic itself).
 */
@Component
@RequiredArgsConstructor
public class DocumentSessionRegistry {

    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;
    private final Map<UUID, DocumentSession> sessions = new ConcurrentHashMap<>();

    public DocumentSession.AppliedOp applyEdit(UUID documentId, Delta op, long baseVersion) {
        DocumentSession session = sessions.computeIfAbsent(documentId, this::loadSession);
        DocumentSession.AppliedOp applied = session.apply(op, baseVersion);
        persist(documentId, session);
        return applied;
    }

    @SneakyThrows
    private DocumentSession loadSession(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        List<Map<String, Object>> json = objectMapper.readValue(
                document.getContent(), new TypeReference<List<Map<String, Object>>>() {
                });
        return new DocumentSession(Delta.fromJson(json), document.getVersion());
    }

    @SneakyThrows
    private void persist(UUID documentId, DocumentSession session) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        document.setContent(objectMapper.writeValueAsString(session.content().toJson()));
        document.setVersion(session.version());
        documentRepository.save(document);
    }
}
