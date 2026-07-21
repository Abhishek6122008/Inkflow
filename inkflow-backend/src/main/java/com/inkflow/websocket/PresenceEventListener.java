package com.inkflow.websocket;

import com.inkflow.entity.User;
import com.inkflow.ot.PresenceEntry;
import com.inkflow.ot.PresenceMessage;
import com.inkflow.ot.PresenceService;
import com.inkflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks which document each STOMP subscription belongs to (so DISCONNECT,
 * which carries no destination, can still resolve which document(s) to
 * remove the session's presence from), and drives {@link PresenceService}
 * join/leave + broadcast on subscribe/unsubscribe/disconnect of
 * {@code /topic/doc/{id}}.
 */
@Component
@RequiredArgsConstructor
public class PresenceEventListener {

    private static final Pattern DOC_TOPIC = Pattern.compile("^/topic/doc/([0-9a-fA-F-]{36})$");

    private final PresenceService presenceService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private record Subscription(String sessionId, String subscriptionId) {
    }

    private final Map<Subscription, UUID> documentBySubscription = new ConcurrentHashMap<>();

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String subscriptionId = accessor.getSubscriptionId();
        String sessionId = accessor.getSessionId();
        if (destination == null || subscriptionId == null || sessionId == null) return;

        Matcher matcher = DOC_TOPIC.matcher(destination);
        if (!matcher.matches()) return;

        UUID documentId = UUID.fromString(matcher.group(1));
        String presenceKey = sessionId + ":" + subscriptionId;
        resolveUser(accessor).ifPresent(user -> {
            documentBySubscription.put(new Subscription(sessionId, subscriptionId), documentId);
            broadcast(documentId, presenceService.join(documentId, presenceKey, user.getId(), user.getDisplayName()));
        });
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String subscriptionId = accessor.getSubscriptionId();
        String sessionId = accessor.getSessionId();
        if (subscriptionId == null || sessionId == null) return;
        removeSubscription(new Subscription(sessionId, subscriptionId));
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) return;

        documentBySubscription.keySet().stream()
                .filter(sub -> sub.sessionId().equals(sessionId))
                .toList()
                .forEach(this::removeSubscription);
    }

    private void removeSubscription(Subscription subscription) {
        UUID documentId = documentBySubscription.remove(subscription);
        if (documentId != null) {
            String presenceKey = subscription.sessionId() + ":" + subscription.subscriptionId();
            broadcast(documentId, presenceService.leave(documentId, presenceKey));
        }
    }

    private Optional<User> resolveUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) return Optional.empty();
        return userRepository.findByEmail(accessor.getUser().getName());
    }

    private void broadcast(UUID documentId, List<PresenceEntry> users) {
        messagingTemplate.convertAndSend(
                "/topic/doc/" + documentId + "/presence",
                new PresenceMessage(documentId, users)
        );
    }
}
