package se.edugrade.monsterhuntingboard.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.edugrade.monsterhuntingboard.dto.ChatMessageRequest;
import se.edugrade.monsterhuntingboard.dto.ChatMessageResponse;
import se.edugrade.monsterhuntingboard.service.ChatService;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/global/recent")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<List<ChatMessageResponse>> getRecentGlobalMessages(Principal principal) {
        return ResponseEntity.ok(chatService.getRecentGlobalMessages(principal.getName()));
    }

    @PostMapping("/global")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<ChatMessageResponse> sendGlobalMessage(
            Principal principal,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendGlobalMessage(principal.getName(), request));
    }

    @GetMapping("/lobby/{lobbyId}/recent")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<List<ChatMessageResponse>> getRecentLobbyMessages(
            @PathVariable Long lobbyId,
            Principal principal
    ) {
        return ResponseEntity.ok(chatService.getRecentLobbyMessages(lobbyId, principal.getName()));
    }

    @PostMapping("/lobby/{lobbyId}")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<ChatMessageResponse> sendLobbyMessage(
            @PathVariable Long lobbyId,
            Principal principal,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendLobbyMessage(lobbyId, principal.getName(), request));
    }
}
