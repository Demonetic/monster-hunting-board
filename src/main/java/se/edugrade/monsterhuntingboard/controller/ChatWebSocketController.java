package se.edugrade.monsterhuntingboard.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import se.edugrade.monsterhuntingboard.dto.ChatMessageRequest;
import se.edugrade.monsterhuntingboard.dto.ChatMessageResponse;
import se.edugrade.monsterhuntingboard.dto.ErrorResponse;
import se.edugrade.monsterhuntingboard.service.ChatService;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/global")
    @PreAuthorize("hasRole('HUNTER')")
    public void sendGlobalMessage(
            Principal principal,
            @Valid @Payload ChatMessageRequest request
    ) {
        ChatMessageResponse message = chatService.sendGlobalMessage(principal.getName(), request);
        messagingTemplate.convertAndSend("/topic/chat/global", message);
    }

    @MessageMapping("/chat/lobby/{lobbyId}")
    @PreAuthorize("hasRole('HUNTER')")
    public void sendLobbyMessage(
            @DestinationVariable Long lobbyId,
            Principal principal,
            @Valid @Payload ChatMessageRequest request
    ) {
        ChatMessageResponse message = chatService.sendLobbyMessage(lobbyId, principal.getName(), request);
        messagingTemplate.convertAndSend("/topic/chat/lobby/" + lobbyId, message);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/chat/errors")
    public ErrorResponse handleChatError(Exception exception) {
        return ErrorResponse.from(HttpStatus.BAD_REQUEST, exception.getMessage(), "/ws");
    }
}
