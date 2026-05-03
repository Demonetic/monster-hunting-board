package se.edugrade.monsterhuntingboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.util.TestIds;
import se.edugrade.monsterhuntingboard.dto.CompleteHuntRequest;
import se.edugrade.monsterhuntingboard.dto.CreateHuntRequest;
import se.edugrade.monsterhuntingboard.dto.HuntResponse;
import se.edugrade.monsterhuntingboard.dto.HuntResultResponse;
import se.edugrade.monsterhuntingboard.dto.JoinHuntResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateHuntRequest;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.PartyFullException;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;
import se.edugrade.monsterhuntingboard.repository.BeastRepository;
import se.edugrade.monsterhuntingboard.repository.HuntParticipationRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;
import se.edugrade.monsterhuntingboard.repository.UserAccountRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HuntServiceTest {

    @Autowired
    private HuntService huntService;

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

    @MockitoBean
    private BattleService battleService;

    private Beast beast;
    private Hunt activeHunt;
    private String hunterOneUsername;
    private String hunterTwoUsername;

    @BeforeEach
    void setUp() {
        given(battleService.rollWin()).willReturn(true);

        beast = beastRepository.save(Beast.builder()
                .type(BeastType.GRIFFIN)
                .difficulty(Difficulty.MEDIUM)
                .hp(180)
                .attackPower(35)
                .rewardExp(100)
                .rewardGold(75)
                .build());

        activeHunt = huntRepository.save(Hunt.builder()
                .title("Griffin Hunt at Dawn")
                .type(HuntType.HUNT)
                .difficulty(Difficulty.MEDIUM)
                .status(HuntStatus.ACTIVE)
                .startTime(LocalDateTime.now().plusHours(2))
                .maxPartySize(4)
                .beasts(List.of(beast))
                .rewardExp(100)
                .rewardGold(75)
                .build());

        hunterOneUsername = "h1-" + TestIds.shortId();
        hunterTwoUsername = "h2-" + TestIds.shortId();
        saveHunter(hunterOneUsername, "Aria", Appearance.MAGE);
        saveHunter(hunterTwoUsername, "Rowan", Appearance.RANGER);
    }

    @Test
    void createHuntWorks() {
        CreateHuntRequest request = new CreateHuntRequest(
                "Test Hunt",
                HuntType.HUNT,
                Difficulty.EASY,
                HuntStatus.SCHEDULED,
                LocalDateTime.now().plusHours(3),
                3,
                List.of(beast.getId()),
                50,
                25
        );

        HuntResponse response = huntService.createHunt(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isEqualTo("Test Hunt");
        assertThat(response.type()).isEqualTo(HuntType.HUNT);
        assertThat(response.beasts()).hasSize(1);
        assertThat(response.currentPartySize()).isEqualTo(0);
    }

    @Test
    void joinHuntWorksAndFullPartyIsRejected() {
        JoinHuntResponse response = huntService.joinHunt(activeHunt.getId(), hunterOneUsername);

        assertThat(response.huntId()).isEqualTo(activeHunt.getId());
        assertThat(response.hunterDisplayName()).isEqualTo("Aria");
        assertThat(response.currentPartySize()).isEqualTo(1);

        Hunt fullHunt = huntRepository.save(Hunt.builder()
                .title("Full Hunt")
                .type(HuntType.HUNT)
                .difficulty(Difficulty.EASY)
                .status(HuntStatus.ACTIVE)
                .startTime(LocalDateTime.now().plusHours(1))
                .maxPartySize(1)
                .beasts(List.of(beast))
                .rewardExp(50)
                .rewardGold(25)
                .build());

        huntService.joinHunt(fullHunt.getId(), hunterOneUsername);
        assertThatThrownBy(() -> huntService.joinHunt(fullHunt.getId(), hunterTwoUsername))
                .isInstanceOf(PartyFullException.class);
    }

    @Test
    void completeHuntWithWinRollGivesReward() {
        huntService.joinHunt(activeHunt.getId(), hunterOneUsername);

        HuntResultResponse response = huntService.completeHuntForCurrentHunter(
                activeHunt.getId(),
                hunterOneUsername,
                new CompleteHuntRequest(true)
        );

        assertThat(response.won()).isTrue();
        assertThat(response.expChange()).isEqualTo(100);
        assertThat(response.goldChange()).isEqualTo(75);
        assertThat(response.newLevel()).isEqualTo(2);
        assertThat(response.newBaseHp()).isEqualTo(110);
    }

    @Test
    void completeHuntWithLossRollGivesPenalty() {
        given(battleService.rollWin()).willReturn(false);
        huntService.joinHunt(activeHunt.getId(), hunterOneUsername);

        HuntResultResponse response = huntService.completeHuntForCurrentHunter(
                activeHunt.getId(),
                hunterOneUsername,
                new CompleteHuntRequest(true)
        );

        assertThat(response.won()).isFalse();
        assertThat(response.expChange()).isEqualTo(-25);
        assertThat(response.goldChange()).isEqualTo(0);
        assertThat(response.newLevel()).isEqualTo(1);
        assertThat(response.newBaseHp()).isEqualTo(100);
    }

    @Test
    void startSoloHuntCoversWinAndLossOutcomes() {
        Hunt soloHunt = huntRepository.save(Hunt.builder()
                .title("Solo Trial")
                .type(HuntType.SOLO_HUNT)
                .difficulty(Difficulty.EASY)
                .status(HuntStatus.ACTIVE)
                .beasts(List.of(beast))
                .rewardExp(50)
                .rewardGold(25)
                .build());

        HuntResultResponse winResponse = huntService.startSoloHunt(
                soloHunt.getId(),
                hunterOneUsername,
                new CompleteHuntRequest(true)
        );
        assertThat(winResponse.won()).isTrue();
        assertThat(winResponse.expChange()).isEqualTo(50);

        given(battleService.rollWin()).willReturn(false);
        Hunt hardSoloHunt = huntRepository.save(Hunt.builder()
                .title("Hard Solo Trial")
                .type(HuntType.SOLO_HUNT)
                .difficulty(Difficulty.HARD)
                .status(HuntStatus.ACTIVE)
                .beasts(List.of(beast))
                .rewardExp(50)
                .rewardGold(25)
                .build());

        HuntResultResponse lossResponse = huntService.startSoloHunt(
                hardSoloHunt.getId(),
                hunterOneUsername,
                new CompleteHuntRequest(true)
        );
        assertThat(lossResponse.won()).isFalse();
        assertThat(lossResponse.expChange()).isEqualTo(-50);
    }

    @Test
    void updateAndDeleteRulesAreEnforced() {
        HuntResponse updated = huntService.updateHunt(
                activeHunt.getId(),
                new UpdateHuntRequest("Updated Hunt", null, null, null, null, null, 150, 90)
        );
        assertThat(updated.title()).isEqualTo("Updated Hunt");
        assertThat(updated.rewardExp()).isEqualTo(150);

        assertThatThrownBy(() -> huntService.updateHunt(
                activeHunt.getId(),
                new UpdateHuntRequest(null, null, null, null, null, List.of(), null, null)
        )).isInstanceOf(InvalidGameRuleException.class);

        Hunt deletableHunt = huntRepository.save(Hunt.builder()
                .title("Delete Hunt")
                .type(HuntType.HUNT)
                .difficulty(Difficulty.EASY)
                .status(HuntStatus.SCHEDULED)
                .startTime(LocalDateTime.now().plusHours(1))
                .maxPartySize(2)
                .beasts(List.of(beast))
                .rewardExp(50)
                .rewardGold(25)
                .build());
        huntService.deleteHunt(deletableHunt.getId());
        assertThat(huntRepository.existsById(deletableHunt.getId())).isFalse();

        huntService.joinHunt(activeHunt.getId(), hunterOneUsername);
        assertThatThrownBy(() -> huntService.deleteHunt(activeHunt.getId()))
                .isInstanceOf(InvalidGameRuleException.class);
    }

    private void saveHunter(String username, String displayName, Appearance appearance) {
        UserAccount userAccount = UserAccount.builder()
                .username(username)
                .password(passwordEncoder.encode("password123"))
                .role(Role.HUNTER)
                .build();

        Hunter hunter = Hunter.builder()
                .displayName(displayName)
                .appearance(appearance)
                .level(1)
                .exp(0)
                .gold(0)
                .baseHp(100)
                .currentHp(100)
                .userAccount(userAccount)
                .build();

        userAccount.setHunter(hunter);
        hunter.setUserAccount(userAccount);
        userAccountRepository.save(userAccount);
    }
}
