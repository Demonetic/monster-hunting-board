package se.edugrade.monsterhuntingboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntParticipation;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.WeatherCategory;
import se.edugrade.monsterhuntingboard.model.WeatherContext;
import se.edugrade.monsterhuntingboard.model.WeatherEffect;

class BattleServiceBalanceTest {

    private static final WeatherEffect NEUTRAL_WEATHER = WeatherEffect.neutral();
    private final BattleService battleService = new BattleService();

    @Test
    void easyAndMediumRemainWinnableWhileHardStaysDangerous() {
        BattleStats easyStats = simulateSoloBattles(
                soloHunt(Difficulty.EASY, beast("Basilisk", BeastType.BASILISK, 110, 18)),
                hunter(1L, "Aria", Appearance.MAGE, 1, 100, false),
                450
        );
        BattleStats mediumStats = simulateSoloBattles(
                soloHunt(Difficulty.MEDIUM, beast("Griffin", BeastType.GRIFFIN, 180, 32)),
                hunter(1L, "Aria", Appearance.MAGE, 1, 100, true),
                450
        );
        BattleStats hardStats = simulateSoloBattles(
                soloHunt(Difficulty.HARD, beast("Chimera", BeastType.CHIMERA, 260, 35)),
                hunter(1L, "Aria", Appearance.MAGE, 3, 120, false),
                450
        );

        assertThat(easyStats.winRate()).isGreaterThan(0.8);
        assertThat(easyStats.averageRemainingHpOnWins()).isBetween(40.0, 75.0);

        assertThat(mediumStats.winRate()).isGreaterThan(0.65);
        assertThat(mediumStats.averageRemainingHpOnWins()).isBetween(12.0, 40.0);

        assertThat(hardStats.winRate()).isBetween(0.25, 0.7);
        assertThat(hardStats.averageRemainingHpOnWins()).isLessThan(30.0);
    }

    @Test
    void bossScalingStaysSmoothAsPartySizeIncreases() {
        Hunt bossHunt = bossHunt();

        GroupBattleSimulation duoSimulation = battleService.simulateGroupBossBattle(
                bossHunt,
                participations(
                        hunter(1L, "Aria", Appearance.MAGE, 2, 110, false),
                        hunter(2L, "Rowan", Appearance.RANGER, 2, 110, false)
                ),
                weatherContexts(
                        hunter(1L, "Aria", Appearance.MAGE, 2, 110, false),
                        hunter(2L, "Rowan", Appearance.RANGER, 2, 110, false)
                )
        );
        GroupBattleSimulation partySimulation = battleService.simulateGroupBossBattle(
                bossHunt,
                participations(
                        hunter(1L, "Aria", Appearance.MAGE, 2, 110, false),
                        hunter(2L, "Rowan", Appearance.RANGER, 2, 110, false),
                        hunter(3L, "Brom", Appearance.KNIGHT, 2, 110, true),
                        hunter(4L, "Lyra", Appearance.HUNTER, 2, 110, false)
                ),
                weatherContexts(
                        hunter(1L, "Aria", Appearance.MAGE, 2, 110, false),
                        hunter(2L, "Rowan", Appearance.RANGER, 2, 110, false),
                        hunter(3L, "Brom", Appearance.KNIGHT, 2, 110, true),
                        hunter(4L, "Lyra", Appearance.HUNTER, 2, 110, false)
                )
        );

        assertThat(partySimulation.initialBossHp()).isGreaterThan(duoSimulation.initialBossHp());
        assertThat(partySimulation.initialBossHp()).isLessThan(Math.round(duoSimulation.initialBossHp() * 1.9f));
    }

    @Test
    void soloBattleHpComesFromHuntDifficultyInsteadOfBeastHp() {
        Beast reusedBeast = beast("Reusable Beast", BeastType.DRAGON, 9_999, 18);
        Hunter hunter = hunter(1L, "Aria", Appearance.MAGE, 1, 100, false);

        assertThat(battleService.simulateSoloBattle(soloHunt(Difficulty.EASY, reusedBeast), hunter, NEUTRAL_WEATHER)
                .initialMonsterHp()).isEqualTo(80);
        assertThat(battleService.simulateSoloBattle(soloHunt(Difficulty.MEDIUM, reusedBeast), hunter, NEUTRAL_WEATHER)
                .initialMonsterHp()).isEqualTo(130);
        assertThat(battleService.simulateSoloBattle(soloHunt(Difficulty.HARD, reusedBeast), hunter, NEUTRAL_WEATHER)
                .initialMonsterHp()).isEqualTo(190);
    }

    @Test
    void bossBattleHpComesFromDifficultyLevelAndPartySizeInsteadOfBeastHp() {
        Hunt bossHunt = Hunt.builder()
                .title("Reusable Boss Hunt")
                .type(HuntType.HUNT)
                .difficulty(Difficulty.BOSS)
                .status(HuntStatus.ACTIVE)
                .beasts(List.of(beast("Reusable Beast", BeastType.DRAGON, 9_999, 82)))
                .rewardExp(420)
                .rewardGold(520)
                .build();
        Hunter firstHunter = hunter(1L, "Aria", Appearance.MAGE, 2, 110, false);
        Hunter secondHunter = hunter(2L, "Rowan", Appearance.RANGER, 2, 110, false);

        GroupBattleSimulation simulation = battleService.simulateGroupBossBattle(
                bossHunt,
                participations(firstHunter, secondHunter),
                weatherContexts(firstHunter, secondHunter)
        );

        assertThat(simulation.initialBossHp()).isEqualTo(479);
    }

    private BattleStats simulateSoloBattles(Hunt hunt, Hunter hunterTemplate, int iterations) {
        int wins = 0;
        int totalRemainingHpOnWins = 0;

        for (int index = 0; index < iterations; index++) {
            Hunter hunter = hunter(
                    hunterTemplate.getId(),
                    hunterTemplate.getDisplayName(),
                    hunterTemplate.getAppearance(),
                    hunterTemplate.getLevel(),
                    hunterTemplate.getBaseHp(),
                    hunterTemplate.isEndurancePotionActive()
            );
            SoloBattleSimulation simulation = battleService.simulateSoloBattle(hunt, hunter, NEUTRAL_WEATHER);
            if (simulation.hunterWon()) {
                wins++;
                totalRemainingHpOnWins += simulation.hunterRemainingHp();
            }
        }

        double winRate = wins / (double) iterations;
        double averageRemainingHpOnWins = wins == 0 ? 0.0 : totalRemainingHpOnWins / (double) wins;
        return new BattleStats(winRate, averageRemainingHpOnWins);
    }

    private Hunt soloHunt(Difficulty difficulty, Beast beast) {
        return Hunt.builder()
                .title(difficulty + " Hunt")
                .type(HuntType.SOLO_HUNT)
                .difficulty(difficulty)
                .status(HuntStatus.ACTIVE)
                .beasts(List.of(beast))
                .rewardExp(100)
                .rewardGold(80)
                .build();
    }

    private Hunt bossHunt() {
        return Hunt.builder()
                .title("Dragonfall Vanguard")
                .type(HuntType.HUNT)
                .difficulty(Difficulty.BOSS)
                .status(HuntStatus.ACTIVE)
                .beasts(List.of(beast("Dragon", BeastType.DRAGON, 520, 82)))
                .rewardExp(420)
                .rewardGold(520)
                .build();
    }

    private Beast beast(String name, BeastType type, int hp, int attackPower) {
        return Beast.builder()
                .name(name)
                .type(type)
                .hp(hp)
                .attackPower(attackPower)
                .rewardExp(100)
                .rewardGold(80)
                .build();
    }

    private Hunter hunter(Long id, String name, Appearance appearance, int level, int baseHp, boolean endurancePotionActive) {
        return Hunter.builder()
                .id(id)
                .displayName(name)
                .appearance(appearance)
                .level(level)
                .exp(0)
                .gold(0)
                .baseHp(baseHp)
                .currentHp(baseHp)
                .endurancePotionActive(endurancePotionActive)
                .expPotionActive(false)
                .build();
    }

    private List<HuntParticipation> participations(Hunter... hunters) {
        return java.util.stream.IntStream.range(0, hunters.length)
                .mapToObj(index -> HuntParticipation.builder()
                        .id((long) index + 1)
                        .hunter(hunters[index])
                        .joinedAt(LocalDateTime.now().plusSeconds(index))
                        .completed(false)
                        .won(false)
                        .expChange(0)
                        .goldChange(0)
                        .build())
                .toList();
    }

    private Map<Long, GroupParticipantBattleContext> weatherContexts(Hunter... hunters) {
        Map<Long, GroupParticipantBattleContext> contexts = new LinkedHashMap<>();
        for (Hunter hunter : hunters) {
            contexts.put(
                    hunter.getId(),
                    new GroupParticipantBattleContext(
                            hunter.getId(),
                            hunter.getDisplayName(),
                            new WeatherContext(
                                    hunter.getCity(),
                                    hunter.getCountry(),
                                    0,
                                    0,
                                    0,
                                    0,
                                    12.0,
                                    false,
                                    WeatherCategory.CLOUDY_OVERCAST,
                                    NEUTRAL_WEATHER
                            )
                    )
            );
        }
        return contexts;
    }

    private record BattleStats(double winRate, double averageRemainingHpOnWins) {
    }
}
