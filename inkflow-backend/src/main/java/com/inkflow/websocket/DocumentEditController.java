package com.inkflow.websocket;

import com.inkflow.entity.User;
import com.inkflow.exception.DocumentAccessDeniedException;
import com.inkflow.exception.DocumentNotFoundException;
import com.inkflow.ot.BroadcastMessage;
import com.inkflow.ot.Delta;
import com.inkflow.ot.DocumentSession;
import com.inkflow.ot.DocumentSessionRegistry;
import com.inkflow.ot.EditErrorMessage;
import com.inkflow.ot.EditMessage;
import com.inkflow.repository.UserRepository;
import com.inkflow.service.DocumentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DocumentEditController {

    private final DocumentService documentService;
    private final DocumentSessionRegistry sessionRegistry;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/doc/{documentId}/edit")
    public void edit(
            @DestinationVariable UUID documentId,
            EditMessage message,
            Authentication authentication
    ) {
        String email = authentication.getName();
        try {
            UUID authorId = documentService.resolveEditorUserId(email, documentId);
            Delta incomingOp = Delta.fromJson(message.op());

            DocumentSession.AppliedOp applied = sessionRegistry.applyEdit(documentId, incomingOp, message.baseVersion());

            BroadcastMessage broadcast = new BroadcastMessage(
                    documentId, applied.op().toJson(), applied.version(), authorId
            );
            messagingTemplate.convertAndSend("/topic/doc/" + documentId, broadcast);
        } catch (DocumentNotFoundException | DocumentAccessDeniedException | EntityNotFoundException e) {
            sendError(email, documentId, e);
        }
    }

    private void sendError(String email, UUID documentId, RuntimeException e) {
        String code = switch (e) {
            case DocumentNotFoundException ignored -> "DOCUMENT_NOT_FOUND";
            case DocumentAccessDeniedException ignored -> "DOCUMENT_ACCESS_DENIED";
            default -> "EDIT_REJECTED";
        };
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/errors",
                new EditErrorMessage(documentId, code, e.getMessage())
        );
    }
}
