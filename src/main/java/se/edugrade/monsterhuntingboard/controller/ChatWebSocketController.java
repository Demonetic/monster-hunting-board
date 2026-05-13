package se.edugrade.monsterhuntingboard.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import se.edugrade.monsterhuntingboard.dto.ChatMessageRequest;
import se.edugrade.monsterhuntingboard.dto.ChatMessageResponse;
import se.edugrade.monsterhuntingboard.dto.ErrorResponse;
import se.edugrade.monsterhuntingboard.exception.UnauthorizedActionException;
import se.edugrade.monsterhuntingboard.service.ChatService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/global")
    public void sendGlobalMessage(
            Principal principal,
            @Valid @Payload ChatMessageRequest request
    ) {
        String username = requireAuthenticatedUsername(principal);
        log.info("Global chat SEND: username={}", username);
        ChatMessageResponse message = chatService.sendGlobalMessage(username, request);
        messagingTemplate.convertAndSend("/topic/chat/global", message);
    }

    @MessageMapping("/chat/lobby/{lobbyId}")
    public void sendLobbyMessage(
            @DestinationVariable Long lobbyId,
            Principal principal,
            @Valid @Payload ChatMessageRequest request
    ) {
        String username = requireAuthenticatedUsername(principal);
        log.info("Lobby chat SEND: username={}, lobbyId={}", username, lobbyId);
        ChatMessageResponse message = chatService.sendLobbyMessage(lobbyId, username, request);
        messagingTemplate.convertAndSend("/topic/chat/lobby/" + lobbyId, message);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/chat/errors")
    public ErrorResponse handleChatError(Exception exception) {
        log.warn("Chat websocket error: {}", exception.getMessage());
        return ErrorResponse.from(HttpStatus.BAD_REQUEST, exception.getMessage(), "/ws");
    }

    private String requireAuthenticatedUsername(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedActionException("WebSocket authentication is required");
        }

        return principal.getName();
    }
}
