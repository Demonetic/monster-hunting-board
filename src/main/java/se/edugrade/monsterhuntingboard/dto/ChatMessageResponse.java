package se.edugrade.monsterhuntingboard.dto;

import java.time.LocalDateTime;
import se.edugrade.monsterhuntingboard.model.ChatMessage;
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
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getSenderHunterId(),
                chatMessage.getSenderDisplayName(),
                chatMessage.getMessageText(),
                chatMessage.getChatType(),
                chatMessage.getLobbyId(),
                chatMessage.getCreatedAt()
        );
    }
}
