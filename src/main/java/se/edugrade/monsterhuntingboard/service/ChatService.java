package se.edugrade.monsterhuntingboard.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.ChatMessageRequest;
import se.edugrade.monsterhuntingboard.dto.ChatMessageResponse;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.InvalidHuntStateException;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.exception.UnauthorizedActionException;
import se.edugrade.monsterhuntingboard.model.ChatType;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.repository.HuntParticipationRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;
import se.edugrade.monsterhuntingboard.repository.HunterRepository;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final int RECENT_MESSAGE_LIMIT = 50;
    private static final Duration MESSAGE_RETENTION = Duration.ofHours(1);
    private static final int MAX_MESSAGE_LENGTH = 250;
    private static final Duration MIN_TIME_BETWEEN_MESSAGES = Duration.ofSeconds(1);

    private final HunterRepository hunterRepository;
    private final HuntRepository huntRepository;
    private final HuntParticipationRepository huntParticipationRepository;
    private final AtomicLong nextMessageId = new AtomicLong(1);
    private final Deque<ChatMessageResponse> recentGlobalMessages = new ArrayDeque<>();
    private final Map<Long, Deque<ChatMessageResponse>> recentLobbyMessagesByLobbyId = new HashMap<>();
    private final Map<Long, Instant> lastMessageSentByHunterId = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentGlobalMessages(String username) {
        requireHunter(username);
        synchronized (recentGlobalMessages) {
            removeExpiredMessages(recentGlobalMessages);
            return List.copyOf(recentGlobalMessages);
        }
    }

    @Transactional
    public ChatMessageResponse sendGlobalMessage(String username, ChatMessageRequest request) {
        Hunter sender = requireHunter(username);
        String messageText = normalizeMessage(request.message());
        enforceRateLimit(sender);

        ChatMessageResponse message = createMessage(sender, messageText, ChatType.GLOBAL, null);

        synchronized (recentGlobalMessages) {
            addRecentMessage(recentGlobalMessages, message);
        }

        return message;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentLobbyMessages(Long lobbyId, String username) {
        Hunter sender = requireHunter(username);
        requireLobbyParticipant(lobbyId, sender);

        synchronized (recentLobbyMessagesByLobbyId) {
            Deque<ChatMessageResponse> recentLobbyMessages = recentLobbyMessagesByLobbyId.getOrDefault(
                    lobbyId,
                    new ArrayDeque<>()
            );
            removeExpiredMessages(recentLobbyMessages);
            return List.copyOf(recentLobbyMessages);
        }
    }

    @Transactional(readOnly = true)
    public void validateLobbySubscription(Long lobbyId, String username) {
        Hunter sender = requireHunter(username);
        requireLobbyParticipant(lobbyId, sender);
    }

    @Transactional
    public ChatMessageResponse sendLobbyMessage(Long lobbyId, String username, ChatMessageRequest request) {
        Hunter sender = requireHunter(username);
        Hunt lobby = requireOpenLobby(lobbyId, sender);
        String messageText = normalizeMessage(request.message());
        enforceRateLimit(sender);

        ChatMessageResponse message = createMessage(sender, messageText, ChatType.LOBBY, lobby.getId());

        synchronized (recentLobbyMessagesByLobbyId) {
            Deque<ChatMessageResponse> recentLobbyMessages = recentLobbyMessagesByLobbyId.computeIfAbsent(
                    lobby.getId(),
                    ignored -> new ArrayDeque<>()
            );
            addRecentMessage(recentLobbyMessages, message);
        }

        return message;
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

    private ChatMessageResponse createMessage(
            Hunter sender,
            String messageText,
            ChatType chatType,
            Long lobbyId
    ) {
        return new ChatMessageResponse(
                nextMessageId.getAndIncrement(),
                sender.getId(),
                sender.getDisplayName(),
                messageText,
                chatType,
                lobbyId,
                LocalDateTime.now()
        );
    }

    private void addRecentMessage(Deque<ChatMessageResponse> messages, ChatMessageResponse message) {
        removeExpiredMessages(messages);
        messages.addLast(message);

        while (messages.size() > RECENT_MESSAGE_LIMIT) {
            messages.removeFirst();
        }
    }

    private void removeExpiredMessages(Deque<ChatMessageResponse> messages) {
        LocalDateTime oldestAllowedCreatedAt = LocalDateTime.now().minus(MESSAGE_RETENTION);

        while (!messages.isEmpty() && messages.getFirst().createdAt().isBefore(oldestAllowedCreatedAt)) {
            messages.removeFirst();
        }
    }
}
