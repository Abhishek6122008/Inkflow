package com.inkflow.ot;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks who's currently subscribed to each document's edit topic, backed by
 * a Redis hash per document ({@code presence:doc:{documentId}}, field =
 * STOMP subscription id, value = "{userId}:{displayName}") so presence
 * naturally survives a backend restart and is ready to share across
 * instances later (see architecture.md "Real-time sync" — the OT engine
 * itself is still single-instance only, presence does not need to be).
 * One Redis hash field per subscription (not per user) so a user with two
 * tabs open still shows as present after closing one of them.
 */
@Component
@RequiredArgsConstructor
public class PresenceService {

    private static final Duration ENTRY_TTL = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;

    public List<PresenceEntry> join(UUID documentId, String subscriptionId, UUID userId, String displayName) {
        String key = key(documentId);
        redisTemplate.opsForHash().put(key, subscriptionId, userId + ":" + displayName);
        redisTemplate.expire(key, ENTRY_TTL);
        return activeUsers(documentId);
    }

    public List<PresenceEntry> leave(UUID documentId, String subscriptionId) {
        redisTemplate.opsForHash().delete(key(documentId), subscriptionId);
        return activeUsers(documentId);
    }

    public List<PresenceEntry> activeUsers(UUID documentId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(documentId));
        Map<UUID, String> distinctUsers = new LinkedHashMap<>();
        for (Object value : entries.values()) {
            String[] parts = ((String) value).split(":", 2);
            distinctUsers.put(UUID.fromString(parts[0]), parts[1]);
        }
        return distinctUsers.entrySet().stream()
                .map(e -> new PresenceEntry(e.getKey(), e.getValue()))
                .toList();
    }

    private String key(UUID documentId) {
        return "presence:doc:" + documentId;
    }
}
