package se.edugrade.monsterhuntingboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import se.edugrade.monsterhuntingboard.dto.ChatMessageRequest;
import se.edugrade.monsterhuntingboard.dto.ChatMessageResponse;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.InvalidHuntStateException;
import se.edugrade.monsterhuntingboard.exception.UnauthorizedActionException;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntParticipation;
import se.edugrade.monsterhuntingboard.model.HuntSourceType;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;
import se.edugrade.monsterhuntingboard.repository.BeastRepository;
import se.edugrade.monsterhuntingboard.repository.HuntParticipationRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;
import se.edugrade.monsterhuntingboard.repository.UserAccountRepository;
import se.edugrade.monsterhuntingboard.util.TestIds;

@SpringBootTest
@ActiveProfiles("test")
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private BeastRepository beastRepository;

    @Autowired
    private HuntRepository huntRepository;

    @Autowired
    private HuntParticipationRepository huntParticipationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loggedInHunterCanSendAndReadGlobalMessages() {
        String username = saveHunter("global", "Aria").getUserAccount().getUsername();

        ChatMessageResponse sentMessage = chatService.sendGlobalMessage(username, new ChatMessageRequest(" Hello realm "));
        List<ChatMessageResponse> recentMessages = chatService.getRecentGlobalMessages(username);

        assertThat(sentMessage.messageText()).isEqualTo("Hello realm");
        assertThat(recentMessages)
                .extracting(ChatMessageResponse::messageText)
                .contains("Hello realm");
    }

    @Test
    void emptyAndVeryLongMessagesAreRejected() {
        String username = saveHunter("validation", "Lyra").getUserAccount().getUsername();

        assertThatThrownBy(() -> chatService.sendGlobalMessage(username, new ChatMessageRequest("   ")))
                .isInstanceOf(InvalidGameRuleException.class)
                .hasMessageContaining("empty");

        assertThatThrownBy(() -> chatService.sendGlobalMessage(username, new ChatMessageRequest("x".repeat(251))))
                .isInstanceOf(InvalidGameRuleException.class)
                .hasMessageContaining("250");
    }

    @Test
    void globalChatKeepsOnlyLatestFiftyMessages() {
        String lastUsername = "";

        for (int index = 0; index < 51; index++) {
            lastUsername = saveHunter("retention-" + index, "Ret" + index).getUserAccount().getUsername();
            chatService.sendGlobalMessage(lastUsername, new ChatMessageRequest("retention-" + index));
        }

        List<ChatMessageResponse> recentMessages = chatService.getRecentGlobalMessages(lastUsername);

        assertThat(recentMessages).hasSize(50);
        assertThat(recentMessages)
                .extracting(ChatMessageResponse::messageText)
                .doesNotContain("retention-0")
                .contains("retention-50");
    }

    @Test
    void lobbyParticipantCanSendAndReadLobbyMessages() {
        Hunter participant = saveHunter("participant", "Rowan");
        Hunt lobby = saveLobby("Lobby A", HuntStatus.SCHEDULED);
        join(participant, lobby);

        ChatMessageResponse sentMessage = chatService.sendLobbyMessage(
                lobby.getId(),
                participant.getUserAccount().getUsername(),
                new ChatMessageRequest("Ready")
        );
        List<ChatMessageResponse> recentMessages = chatService.getRecentLobbyMessages(
                lobby.getId(),
                participant.getUserAccount().getUsername()
        );

        assertThat(sentMessage.lobbyId()).isEqualTo(lobby.getId());
        assertThat(recentMessages)
                .extracting(ChatMessageResponse::messageText)
                .containsExactly("Ready");
    }

    @Test
    void nonParticipantCannotReadOrSendLobbyMessages() {
        Hunter participant = saveHunter("member", "Nessa");
        Hunter outsider = saveHunter("outsider", "Mira");
        Hunt lobby = saveLobby("Private Lobby", HuntStatus.SCHEDULED);
        join(participant, lobby);

        assertThatThrownBy(() -> chatService.getRecentLobbyMessages(lobby.getId(), outsider.getUserAccount().getUsername()))
                .isInstanceOf(UnauthorizedActionException.class);

        assertThatThrownBy(() -> chatService.sendLobbyMessage(
                lobby.getId(),
                outsider.getUserAccount().getUsername(),
                new ChatMessageRequest("Can I join?")
        )).isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void lobbyMessagesDoNotLeakBetweenLobbies() {
        Hunter hunterA = saveHunter("split-a", "Sera");
        Hunter hunterB = saveHunter("split-b", "Toma");
        Hunt lobbyA = saveLobby("Lobby A", HuntStatus.SCHEDULED);
        Hunt lobbyB = saveLobby("Lobby B", HuntStatus.SCHEDULED);
        join(hunterA, lobbyA);
        join(hunterB, lobbyB);

        chatService.sendLobbyMessage(lobbyA.getId(), hunterA.getUserAccount().getUsername(), new ChatMessageRequest("A only"));
        chatService.sendLobbyMessage(lobbyB.getId(), hunterB.getUserAccount().getUsername(), new ChatMessageRequest("B only"));

        assertThat(chatService.getRecentLobbyMessages(lobbyA.getId(), hunterA.getUserAccount().getUsername()))
                .extracting(ChatMessageResponse::messageText)
                .containsExactly("A only");
        assertThat(chatService.getRecentLobbyMessages(lobbyB.getId(), hunterB.getUserAccount().getUsername()))
                .extracting(ChatMessageResponse::messageText)
                .containsExactly("B only");
    }

    @Test
    void closedLobbyDoesNotAcceptNewMessages() {
        Hunter hunter = saveHunter("closed", "Iris");
        Hunt lobby = saveLobby("Closed Lobby", HuntStatus.ACTIVE);
        join(hunter, lobby);

        assertThatThrownBy(() -> chatService.sendLobbyMessage(
                lobby.getId(),
                hunter.getUserAccount().getUsername(),
                new ChatMessageRequest("Too late")
        )).isInstanceOf(InvalidHuntStateException.class);
    }

    private Hunter saveHunter(String usernamePrefix, String displayName) {
        String id = TestIds.shortId();
        UserAccount userAccount = UserAccount.builder()
                .username(usernamePrefix + "-" + id)
                .password(passwordEncoder.encode("password"))
                .role(Role.HUNTER)
                .build();

        Hunter hunter = Hunter.builder()
                .displayName(displayName + "-" + id)
                .appearance(Appearance.KNIGHT)
                .level(1)
                .exp(0)
                .gold(0)
                .baseHp(100)
                .currentHp(100)
                .userAccount(userAccount)
                .build();

        userAccount.setHunter(hunter);
        return userAccountRepository.save(userAccount).getHunter();
    }

    private Hunt saveLobby(String title, HuntStatus status) {
        Beast beast = beastRepository.save(Beast.builder()
                .name(title + " Beast")
                .type(BeastType.GRIFFIN)
                .hp(100)
                .attackPower(20)
                .rewardExp(50)
                .rewardGold(25)
                .build());

        return huntRepository.save(Hunt.builder()
                .title(title)
                .type(HuntType.HUNT)
                .sourceType(HuntSourceType.MANUAL)
                .difficulty(Difficulty.BOSS)
                .status(status)
                .startTime(LocalDateTime.now().plusMinutes(5))
                .maxPartySize(4)
                .beasts(List.of(beast))
                .rewardExp(100)
                .rewardGold(50)
                .build());
    }

    private void join(Hunter hunter, Hunt hunt) {
        huntParticipationRepository.save(HuntParticipation.builder()
                .hunter(hunter)
                .hunt(hunt)
                .completed(false)
                .won(false)
                .expChange(0)
                .goldChange(0)
                .build());
    }
}
