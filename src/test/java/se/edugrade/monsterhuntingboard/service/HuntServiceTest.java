package se.edugrade.monsterhuntingboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
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

    @MockBean
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

        assertNotNull(response.id());
        assertEquals("Test Hunt", response.title());
        assertEquals(HuntType.HUNT, response.type());
        assertEquals(1, response.beasts().size());
        assertEquals(0, response.currentPartySize());
    }

    @Test
    void joinHuntWorksAndFullPartyIsRejected() {
        JoinHuntResponse response = huntService.joinHunt(activeHunt.getId(), hunterOneUsername);

        assertEquals(activeHunt.getId(), response.huntId());
        assertEquals("Aria", response.hunterDisplayName());
        assertEquals(1, response.currentPartySize());

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
        assertThrows(PartyFullException.class, () -> huntService.joinHunt(fullHunt.getId(), hunterTwoUsername));
    }

    @Test
    void completeHuntWithWinRollGivesReward() {
        huntService.joinHunt(activeHunt.getId(), hunterOneUsername);

        HuntResultResponse response = huntService.completeHuntForCurrentHunter(
                activeHunt.getId(),
                hunterOneUsername,
                new CompleteHuntRequest(true)
        );

        assertTrue(response.won());
        assertEquals(100, response.expChange());
        assertEquals(75, response.goldChange());
        assertEquals(2, response.newLevel());
        assertEquals(110, response.newBaseHp());
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

        assertFalse(response.won());
        assertEquals(-25, response.expChange());
        assertEquals(0, response.goldChange());
        assertEquals(1, response.newLevel());
        assertEquals(100, response.newBaseHp());
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
        assertTrue(winResponse.won());
        assertEquals(50, winResponse.expChange());

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
        assertFalse(lossResponse.won());
        assertEquals(-50, lossResponse.expChange());
    }

    @Test
    void updateAndDeleteRulesAreEnforced() {
        HuntResponse updated = huntService.updateHunt(
                activeHunt.getId(),
                new UpdateHuntRequest("Updated Hunt", null, null, null, null, null, 150, 90)
        );
        assertEquals("Updated Hunt", updated.title());
        assertEquals(150, updated.rewardExp());

        assertThrows(
                InvalidGameRuleException.class,
                () -> huntService.updateHunt(
                        activeHunt.getId(),
                        new UpdateHuntRequest(null, null, null, null, null, List.of(), null, null)
                )
        );

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
        assertFalse(huntRepository.existsById(deletableHunt.getId()));

        huntService.joinHunt(activeHunt.getId(), hunterOneUsername);
        assertThrows(InvalidGameRuleException.class, () -> huntService.deleteHunt(activeHunt.getId()));
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
