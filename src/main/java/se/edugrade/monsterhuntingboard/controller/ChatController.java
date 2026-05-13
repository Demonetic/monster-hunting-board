package se.edugrade.monsterhuntingboard.controller;

import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    @GetMapping("/lobby/{lobbyId}/recent")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<List<ChatMessageResponse>> getRecentLobbyMessages(
            @PathVariable Long lobbyId,
            Principal principal
    ) {
        return ResponseEntity.ok(chatService.getRecentLobbyMessages(lobbyId, principal.getName()));
    }
}
