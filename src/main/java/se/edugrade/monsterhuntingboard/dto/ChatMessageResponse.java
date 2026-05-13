package se.edugrade.monsterhuntingboard.dto;

import java.time.LocalDateTime;
import se.edugrade.monsterhuntingboard.model.ChatType;

public record ChatMessageResponse(
        Long id,
        Long senderHunterId,
        String senderDisplayName,
        String messageText,
        ChatType chatType,
        Long lobbyId,
        LocalDateTime createdAt
) {
}
