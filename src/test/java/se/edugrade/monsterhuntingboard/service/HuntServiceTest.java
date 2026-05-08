package se.edugrade.monsterhuntingboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.BattleTurnResponse;
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
import se.edugrade.monsterhuntingboard.util.GameBalanceUtil;
import se.edugrade.monsterhuntingboard.util.TestIds;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HuntServiceTest {
    private static final ZoneId STOCKHOLM_ZONE = ZoneId.of("Europe/Stockholm");

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
        given(battleService.simulateSoloBattle(any(), any())).willReturn(
                new SoloBattleSimulation(100, 180, true, 10, 90, List.of())
        );

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
                .difficulty(Difficulty.BOSS)
                .status(HuntStatus.ACTIVE)
                .startTime(futureStockholmTime(4))
                .maxPartySize(4)
                .beasts(List.of(beast))
                .rewardExp(100)
                .rewardGold(75)
                .build());

        hunterOneUsername = "h1-" + TestIds.shortId();
        hunterTwoUsername = "h2-" + TestIds.shortId();
        saveHunter(hunterOneUsername, "Aria", Appearance.KNIGHT);
        saveHunter(hunterTwoUsername, "Rowan", Appearance.RANGER);
        given(battleService.simulateGroupBossBattle(eq(activeHunt), any())).willReturn(defaultGroupWinSimulation());
    }

    @Test
    void createHuntWorks() {
        CreateHuntRequest request = new CreateHuntRequest(
                "Test Hunt",
                HuntType.HUNT,
                Difficulty.BOSS,
                HuntStatus.SCHEDULED,
                futureStockholmTime(5),
                3,
                List.of(beast.getId()),
                50,
                25
        );

        HuntResponse response = huntService.createHunt(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isEqualTo("Test Hunt");
        assertThat(response.type()).isEqualTo(HuntType.HUNT);
        assertThat(response.difficulty()).isEqualTo(Difficulty.BOSS);
        assertThat(response.beasts()).hasSize(1);
        assertThat(response.currentPartySize()).isEqualTo(0);
    }

    @Test
    void createHuntRejectsNonBossPartyHuntAndBossSoloHunt() {
        assertThatThrownBy(() -> huntService.createHunt(new CreateHuntRequest(
                "Invalid Party Hunt",
                HuntType.HUNT,
                Difficulty.HARD,
                HuntStatus.SCHEDULED,
                futureStockholmTime(3),
                4,
                List.of(beast.getId()),
                100,
                50
        ))).isInstanceOf(InvalidGameRuleException.class);

        assertThatThrownBy(() -> huntService.createHunt(new CreateHuntRequest(
                "Invalid Solo Hunt",
                HuntType.SOLO_HUNT,
                Difficulty.BOSS,
                HuntStatus.ACTIVE,
                null,
                null,
                List.of(beast.getId()),
                100,
                50
        ))).isInstanceOf(InvalidGameRuleException.class);
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
                .difficulty(Difficulty.BOSS)
                .status(HuntStatus.ACTIVE)
                .startTime(futureStockholmTime(3))
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
        huntService.joinHunt(activeHunt.getId(), hunterTwoUsername);

        HuntResultResponse response = huntService.completeHuntForCurrentHunter(
                activeHunt.getId(),
                hunterOneUsername,
                new CompleteHuntRequest(true)
        );

        assertThat(response.won()).isTrue();
        assertThat(response.expChange()).isEqualTo(100);
        assertThat(response.goldChange()).isEqualTo(75);
        assertThat(response.newLevel()).isEqualTo(1);
        assertThat(response.newBaseHp()).isEqualTo(100);
        assertThat(response.newCurrentHp()).isEqualTo(82);
        assertThat(response.damageTaken()).isEqualTo(18);
        assertThat(response.turns()).hasSize(3);
    }

    @Test
    void completeHuntWithLossRollGivesPenalty() {
        huntService.joinHunt(activeHunt.getId(), hunterOneUsername);
        huntService.joinHunt(activeHunt.getId(), hunterTwoUsername);
        given(battleService.simulateGroupBossBattle(eq(activeHunt), any())).willReturn(defaultGroupLossSimulation());

        HuntResultResponse response = huntService.completeHuntForCurrentHunter(
                activeHunt.getId(),
                hunterOneUsername,
                new CompleteHuntRequest(true)
        );

        assertThat(response.won()).isFalse();
        assertThat(response.expChange()).isEqualTo(-28);
        assertThat(response.goldChange()).isEqualTo(0);
        assertThat(response.newLevel()).isEqualTo(1);
        assertThat(response.newBaseHp()).isEqualTo(100);
        assertThat(response.newCurrentHp()).isEqualTo(100);
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
        assertThat(winResponse.newCurrentHp()).isEqualTo(90);
        assertThat(winResponse.turns()).isEmpty();

        Hunt hardSoloHunt = huntRepository.save(Hunt.builder()
                .title("Hard Solo Trial")
                .type(HuntType.SOLO_HUNT)
                .difficulty(Difficulty.HARD)
                .status(HuntStatus.ACTIVE)
                .beasts(List.of(beast))
                .rewardExp(50)
                .rewardGold(25)
                .build());
        given(battleService.simulateSoloBattle(eq(hardSoloHunt), any())).willReturn(
                new SoloBattleSimulation(100, 180, false, 100, 0, List.of())
        );

        HuntResultResponse lossResponse = huntService.startSoloHunt(
                hardSoloHunt.getId(),
                hunterOneUsername,
                new CompleteHuntRequest(true)
        );
        assertThat(lossResponse.won()).isFalse();
        assertThat(lossResponse.expChange()).isEqualTo(-28);
        assertThat(lossResponse.newCurrentHp()).isEqualTo(100);
    }

    @Test
    void activePotionsApplyToOneHuntAndAreConsumed() {
        UserAccount userAccount = userAccountRepository.findByUsername(hunterOneUsername).orElseThrow();
        Hunter hunter = userAccount.getHunter();
        Hunter secondHunter = userAccountRepository.findByUsername(hunterTwoUsername).orElseThrow().getHunter();
        hunter.setExpPotionActive(true);
        hunter.setEndurancePotionActive(true);

        huntService.joinHunt(activeHunt.getId(), hunterOneUsername);
        huntService.joinHunt(activeHunt.getId(), hunterTwoUsername);
        given(battleService.simulateGroupBossBattle(eq(activeHunt), any())).willReturn(
                new GroupBattleSimulation(
                        180,
                        true,
                        0,
                        List.of(),
                        Map.of(
                                hunter.getId(), new HunterBattleOutcome(86, 14),
                                secondHunter.getId(), new HunterBattleOutcome(90, 10)
                        )
                )
        );

        HuntResultResponse response = huntService.completeHuntForCurrentHunter(
                activeHunt.getId(),
                hunterOneUsername,
                new CompleteHuntRequest(true)
        );

        assertThat(response.expPotionApplied()).isTrue();
        assertThat(response.endurancePotionApplied()).isTrue();
        assertThat(response.expChange()).isEqualTo(110);
        assertThat(response.newLevel()).isEqualTo(1);
        assertThat(response.newBaseHp()).isEqualTo(100);
        assertThat(response.damageTaken()).isEqualTo(14);
        assertThat(response.newCurrentHp()).isEqualTo(86);
        assertThat(hunter.isExpPotionActive()).isFalse();
        assertThat(hunter.isEndurancePotionActive()).isFalse();
    }

    @Test
    void mageMindOfStudyAddsBonusExpOnWin() {
        String mageUsername = "mage-" + TestIds.shortId();
        saveHunter(mageUsername, "Lyra", Appearance.MAGE);
        Hunter mageHunter = userAccountRepository.findByUsername(mageUsername).orElseThrow().getHunter();
        huntService.joinHunt(activeHunt.getId(), mageUsername);
        given(battleService.simulateGroupBossBattle(eq(activeHunt), any())).willReturn(
                new GroupBattleSimulation(
                        180,
                        true,
                        0,
                        List.of(),
                        Map.of(mageHunter.getId(), new HunterBattleOutcome(90, 10))
                )
        );

        HuntResultResponse response = huntService.completeHuntForCurrentHunter(
                activeHunt.getId(),
                mageUsername,
                new CompleteHuntRequest(true)
        );

        assertThat(response.expChange()).isEqualTo(110);
    }

    @Test
    void repeatableHuntCapsWinsAtFivePerDay() {
        Hunt repeatableHunt = huntRepository.save(Hunt.builder()
                .title("Daily Repeatable")
                .type(HuntType.SOLO_HUNT)
                .difficulty(Difficulty.EASY)
                .status(HuntStatus.ACTIVE)
                .sourceType(HuntSourceType.REPEATABLE)
                .generated(true)
                .availableFrom(currentStockholmTime().minusHours(1))
                .expiresAt(currentStockholmTime().plusHours(6))
                .winLimitPerHunter(5)
                .beasts(List.of(beast))
                .rewardExp(50)
                .rewardGold(25)
                .build());

        for (int attempt = 0; attempt < 5; attempt++) {
            HuntResultResponse response = huntService.startSoloHunt(
                    repeatableHunt.getId(),
                    hunterOneUsername,
                    new CompleteHuntRequest(true)
            );
            assertThat(response.won()).isTrue();
        }

        assertThatThrownBy(() -> huntService.startSoloHunt(
                repeatableHunt.getId(),
                hunterOneUsername,
                new CompleteHuntRequest(true)
        )).isInstanceOf(InvalidGameRuleException.class);
    }

    @Test
    void groupVictoryRewardsAllParticipantsEvenIfDead() {
        huntService.joinHunt(activeHunt.getId(), hunterOneUsername);
        huntService.joinHunt(activeHunt.getId(), hunterTwoUsername);
        Hunter firstHunter = userAccountRepository.findByUsername(hunterOneUsername).orElseThrow().getHunter();
        Hunter secondHunter = userAccountRepository.findByUsername(hunterTwoUsername).orElseThrow().getHunter();

        given(battleService.simulateGroupBossBattle(eq(activeHunt), any())).willReturn(
                new GroupBattleSimulation(
                        180,
                        true,
                        0,
                        List.of(),
                        Map.of(
                                firstHunter.getId(), new HunterBattleOutcome(84, 16),
                                secondHunter.getId(), new HunterBattleOutcome(0, 100)
                        )
                )
        );

        huntService.completeHuntForCurrentHunter(activeHunt.getId(), hunterOneUsername, new CompleteHuntRequest(true));

        Hunter refreshedSecondHunter = userAccountRepository.findByUsername(hunterTwoUsername).orElseThrow().getHunter();
        assertThat(refreshedSecondHunter.getExp()).isEqualTo(100);
        assertThat(refreshedSecondHunter.getGold()).isEqualTo(75);
        assertThat(refreshedSecondHunter.getCurrentHp()).isEqualTo(0);
        assertThat(huntParticipationRepository.findByHunterIdAndHuntId(refreshedSecondHunter.getId(), activeHunt.getId()))
                .get()
                .extracting(participation -> participation.isCompleted(), participation -> participation.isWon())
                .containsExactly(true, true);
    }

    @Test
    void groupLossPenalizesAllParticipantsAndFailsHunt() {
        huntService.joinHunt(activeHunt.getId(), hunterOneUsername);
        huntService.joinHunt(activeHunt.getId(), hunterTwoUsername);
        given(battleService.simulateGroupBossBattle(eq(activeHunt), any())).willReturn(defaultGroupLossSimulation());

        huntService.completeHuntForCurrentHunter(activeHunt.getId(), hunterOneUsername, new CompleteHuntRequest(true));

        Hunter firstHunter = userAccountRepository.findByUsername(hunterOneUsername).orElseThrow().getHunter();
        Hunter secondHunter = userAccountRepository.findByUsername(hunterTwoUsername).orElseThrow().getHunter();
        assertThat(firstHunter.getExp()).isZero();
        assertThat(secondHunter.getExp()).isZero();
        assertThat(firstHunter.getCurrentHp()).isEqualTo(100);
        assertThat(secondHunter.getCurrentHp()).isEqualTo(100);
        assertThat(huntRepository.findById(activeHunt.getId()).orElseThrow().getStatus()).isEqualTo(HuntStatus.FAILED);
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

        assertThatThrownBy(() -> huntService.updateHunt(
                activeHunt.getId(),
                new UpdateHuntRequest(null, Difficulty.HARD, null, null, null, null, null, null)
        )).isInstanceOf(InvalidGameRuleException.class);

        Hunt deletableHunt = huntRepository.save(Hunt.builder()
                .title("Delete Hunt")
                .type(HuntType.HUNT)
                .difficulty(Difficulty.BOSS)
                .status(HuntStatus.SCHEDULED)
                .startTime(futureStockholmTime(3))
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
                .baseHp(GameBalanceUtil.calculateBaseHp(1, appearance))
                .currentHp(GameBalanceUtil.calculateBaseHp(1, appearance))
                .userAccount(userAccount)
                .build();

        userAccount.setHunter(hunter);
        hunter.setUserAccount(userAccount);
        userAccountRepository.save(userAccount);
    }

    private GroupBattleSimulation defaultGroupWinSimulation() {
        Hunter firstHunter = userAccountRepository.findByUsername(hunterOneUsername).orElseThrow().getHunter();
        Hunter secondHunter = userAccountRepository.findByUsername(hunterTwoUsername).orElseThrow().getHunter();

        return new GroupBattleSimulation(
                300,
                true,
                0,
                List.of(
                        createTurn(1, "Aria", "hunter", "GRIFFIN", "beast", 15, 285, 100, 285, "Griffin takes 15 damage", "Aria: 100 HP, Rowan: 100 HP | Boss HP: 285"),
                        createTurn(2, "Rowan", "hunter", "GRIFFIN", "beast", 13, 272, 100, 272, "Griffin takes 13 damage", "Aria: 100 HP, Rowan: 100 HP | Boss HP: 272"),
                        createTurn(3, "Griffin", "beast", "Aria", "hunter", 18, 82, 82, 272, "Aria takes 18 damage", "Aria: 82 HP, Rowan: 100 HP | Boss HP: 272")
                ),
                Map.of(
                        firstHunter.getId(), new HunterBattleOutcome(82, 18),
                        secondHunter.getId(), new HunterBattleOutcome(100, 0)
                )
        );
    }

    private GroupBattleSimulation defaultGroupLossSimulation() {
        Hunter firstHunter = userAccountRepository.findByUsername(hunterOneUsername).orElseThrow().getHunter();
        Hunter secondHunter = userAccountRepository.findByUsername(hunterTwoUsername).orElseThrow().getHunter();

        return new GroupBattleSimulation(
                320,
                false,
                120,
                List.of(
                        createTurn(1, "Griffin", "beast", "Aria", "hunter", 55, 45, 45, 320, "Aria takes 55 damage", "Aria: 45 HP, Rowan: 100 HP | Boss HP: 320"),
                        createTurn(2, "Griffin", "beast", "Rowan", "hunter", 100, 0, 0, 320, "Rowan takes 100 damage", "Aria: 45 HP, Rowan: 0 HP (down) | Boss HP: 320"),
                        createTurn(3, "Griffin", "beast", "Aria", "hunter", 45, 0, 0, 320, "Aria takes 45 damage", "Aria: 0 HP (down), Rowan: 0 HP (down) | Boss HP: 320")
                ),
                Map.of(
                        firstHunter.getId(), new HunterBattleOutcome(0, 100),
                        secondHunter.getId(), new HunterBattleOutcome(0, 100)
                )
        );
    }

    private LocalDateTime currentStockholmTime() {
        return ZonedDateTime.now(STOCKHOLM_ZONE).toLocalDateTime();
    }

    private LocalDateTime futureStockholmTime(int hoursAhead) {
        return currentStockholmTime().plusHours(hoursAhead);
    }

    private BattleTurnResponse createTurn(
            int turnNumber,
            String attacker,
            String attackerSide,
            String target,
            String targetSide,
            int damage,
            int targetHpAfter,
            int hunterHpAfter,
            int beastHpAfter,
            String message,
            String battleState
    ) {
        return new BattleTurnResponse(
                turnNumber,
                attacker,
                attackerSide,
                target,
                targetSide,
                damage,
                targetHpAfter,
                hunterHpAfter,
                beastHpAfter,
                message,
                battleState,
                false,
                false
        );
    }
}
