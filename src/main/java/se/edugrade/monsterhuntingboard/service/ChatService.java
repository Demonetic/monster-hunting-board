package se.edugrade.monsterhuntingboard.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.ChatMessageRequest;
import se.edugrade.monsterhuntingboard.dto.ChatMessageResponse;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.InvalidHuntStateException;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.exception.UnauthorizedActionException;
import se.edugrade.monsterhuntingboard.model.ChatMessage;
import se.edugrade.monsterhuntingboard.model.ChatType;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.repository.ChatMessageRepository;
import se.edugrade.monsterhuntingboard.repository.HuntParticipationRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;
import se.edugrade.monsterhuntingboard.repository.HunterRepository;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final int RECENT_MESSAGE_LIMIT = 50;
    private static final int MAX_MESSAGE_LENGTH = 250;
    private static final Duration MIN_TIME_BETWEEN_MESSAGES = Duration.ofSeconds(1);

    private final ChatMessageRepository chatMessageRepository;
    private final HunterRepository hunterRepository;
    private final HuntRepository huntRepository;
    private final HuntParticipationRepository huntParticipationRepository;
    private final Map<Long, Instant> lastMessageSentByHunterId = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentGlobalMessages(String username) {
        requireHunter(username);
        return toOldestFirst(chatMessageRepository.findByChatTypeOrderByCreatedAtDescIdDesc(
                ChatType.GLOBAL,
                PageRequest.of(0, RECENT_MESSAGE_LIMIT)
        ));
    }

    @Transactional
    public ChatMessageResponse sendGlobalMessage(String username, ChatMessageRequest request) {
        Hunter sender = requireHunter(username);
        String messageText = normalizeMessage(request.message());
        enforceRateLimit(sender);

        ChatMessage savedMessage = chatMessageRepository.save(ChatMessage.builder()
                .senderHunterId(sender.getId())
                .senderDisplayName(sender.getDisplayName())
                .messageText(messageText)
                .chatType(ChatType.GLOBAL)
                .build());

        return ChatMessageResponse.from(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentLobbyMessages(Long lobbyId, String username) {
        Hunter sender = requireHunter(username);
        requireLobbyParticipant(lobbyId, sender);

        return toOldestFirst(chatMessageRepository.findByChatTypeAndLobbyIdOrderByCreatedAtDescIdDesc(
                ChatType.LOBBY,
                lobbyId,
                PageRequest.of(0, RECENT_MESSAGE_LIMIT)
        ));
    }

    @Transactional
    public ChatMessageResponse sendLobbyMessage(Long lobbyId, String username, ChatMessageRequest request) {
        Hunter sender = requireHunter(username);
        Hunt lobby = requireOpenLobby(lobbyId, sender);
        String messageText = normalizeMessage(request.message());
        enforceRateLimit(sender);

        ChatMessage savedMessage = chatMessageRepository.save(ChatMessage.builder()
                .senderHunterId(sender.getId())
                .senderDisplayName(sender.getDisplayName())
                .messageText(messageText)
                .chatType(ChatType.LOBBY)
                .lobbyId(lobby.getId())
                .build());

        return ChatMessageResponse.from(savedMessage);
    }

    private Hunter requireHunter(String username) {
        return hunterRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Hunter not found"));
    }

    private Hunt requireOpenLobby(Long lobbyId, Hunter hunter) {
        Hunt lobby = requireLobbyParticipant(lobbyId, hunter);

        if (lobby.getType() != HuntType.HUNT) {
            throw new InvalidHuntStateException("Chat is only available for group hunt lobbies");
        }

        if (lobby.getStatus() != HuntStatus.SCHEDULED) {
            throw new InvalidHuntStateException("Lobby chat is closed for this hunt");
        }

        return lobby;
    }

    private Hunt requireLobbyParticipant(Long lobbyId, Hunter hunter) {
        Hunt lobby = huntRepository.findById(lobbyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hunt not found"));

        if (!huntParticipationRepository.existsByHunterIdAndHuntId(hunter.getId(), lobbyId)) {
            throw new UnauthorizedActionException("Only lobby participants can use this lobby chat");
        }

        return lobby;
    }

    private String normalizeMessage(String rawMessage) {
        String messageText = rawMessage == null ? "" : rawMessage.trim();

        if (messageText.isEmpty()) {
            throw new InvalidGameRuleException("message cannot be empty");
        }

        if (messageText.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidGameRuleException("message cannot be longer than 250 characters");
        }

        return messageText;
    }

    private void enforceRateLimit(Hunter sender) {
        Instant now = Instant.now();
        Instant previous = lastMessageSentByHunterId.get(sender.getId());

        if (previous != null && Duration.between(previous, now).compareTo(MIN_TIME_BETWEEN_MESSAGES) < 0) {
            throw new InvalidGameRuleException("Please wait before sending another chat message");
        }

        lastMessageSentByHunterId.put(sender.getId(), now);
    }

    private List<ChatMessageResponse> toOldestFirst(List<ChatMessage> messages) {
        return messages.stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt).thenComparing(ChatMessage::getId))
                .map(ChatMessageResponse::from)
                .toList();
    }
}
