package se.edugrade.monsterhuntingboard.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.edugrade.monsterhuntingboard.dto.BattleTurnResponse;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntParticipation;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.WeatherEffect;

@Service
public class BattleService {
    private static final Logger log = LoggerFactory.getLogger(BattleService.class);

    public boolean rollWin() {
        boolean won = Math.random() < 0.7;
        log.debug("Battle roll result: {}", won ? "WIN" : "LOSS");
        return won;
    }

    public int calculateDamageTaken(Hunt hunt, boolean won, boolean endurancePotionActive, WeatherEffect weatherEffect) {
        int baseDamage = hunt.getBeasts()
                .stream()
                .mapToInt(beast -> beast.getAttackPower())
                .sum();

        int difficultyModifier = switch (hunt.getDifficulty()) {
            case EASY -> 6;
            case MEDIUM -> 12;
            case HARD -> 18;
            case BOSS -> 28;
        };

        int scaledDamage = won
                ? Math.max(8, Math.round((baseDamage + difficultyModifier) * 0.45f))
                : Math.max(12, Math.round((baseDamage + difficultyModifier) * 0.7f));

        if (endurancePotionActive) {
            scaledDamage = Math.max(0, Math.round(scaledDamage * 0.7f));
        }

        scaledDamage = applyWeatherOutgoingDamage(scaledDamage, hunt.getDifficulty(), weatherEffect);
        scaledDamage = applyWeatherIncomingDamage(scaledDamage, weatherEffect);

        log.debug("Calculated damage taken for hunt {}: {}", hunt.getId(), scaledDamage);
        return scaledDamage;
    }

    public SoloBattleSimulation simulateSoloBattle(Hunt hunt, Hunter hunter, WeatherEffect weatherEffect) {
        int hunterHp = hunter.getCurrentHp();
        int monsterHp = calculateScaledMonsterHp(hunt, hunter.getLevel());
        int initialHunterHp = hunterHp;
        int initialMonsterHp = monsterHp;
        String beastName = hunt.getBeasts().isEmpty()
                ? "Beast"
                : hunt.getBeasts().getFirst().getType().name();
        boolean hunterTurn = ThreadLocalRandom.current().nextBoolean();
        int turnNumber = 1;
        int totalDamageTaken = 0;
        List<BattleTurnResponse> turns = new ArrayList<>();

        while (hunterHp > 0 && monsterHp > 0) {
            if (hunterTurn) {
                int hunterDamage = rollHunterDamage(hunter.getLevel(), hunter.getAppearance(), weatherEffect);
                monsterHp = Math.max(0, monsterHp - hunterDamage);
                turns.add(new BattleTurnResponse(
                        turnNumber++,
                        hunter.getDisplayName(),
                        "hunter",
                        beastName,
                        "beast",
                        hunterDamage,
                        monsterHp,
                        hunterHp,
                        monsterHp,
                        "%s takes %d damage".formatted(toDisplayName(beastName), hunterDamage),
                        "Hunter HP: %d, Boss HP: %d".formatted(hunterHp, monsterHp)
                        ,
                        false,
                        false
                ));
            } else {
                int monsterDamage = rollMonsterDamage(hunt, hunter.getLevel(), weatherEffect);
                monsterDamage = adjustBeastDamageAgainstHunter(
                        monsterDamage,
                        hunt.getDifficulty(),
                        hunter.getLevel(),
                        hunter.isEndurancePotionActive(),
                        hunter.getAppearance(),
                        weatherEffect
                );
                hunterHp = Math.max(0, hunterHp - monsterDamage);
                totalDamageTaken += monsterDamage;
                turns.add(new BattleTurnResponse(
                        turnNumber++,
                        toDisplayName(beastName),
                        "beast",
                        hunter.getDisplayName(),
                        "hunter",
                        monsterDamage,
                        hunterHp,
                        hunterHp,
                        monsterHp,
                        "%s takes %d damage".formatted(hunter.getDisplayName(), monsterDamage),
                        "Hunter HP: %d, Boss HP: %d".formatted(hunterHp, monsterHp)
                        ,
                        false,
                        false
                ));
            }

            hunterTurn = !hunterTurn;
        }

        WeatherFatigueResult fatigueResult = applyWeatherFatigue(
                weatherEffect,
                hunter.getDisplayName(),
                beastName,
                turnNumber,
                hunterHp,
                monsterHp,
                totalDamageTaken,
                turns
        );

        return new SoloBattleSimulation(
                initialHunterHp,
                initialMonsterHp,
                fatigueResult.remainingHp() > 0 && monsterHp <= 0,
                fatigueResult.totalDamageTaken(),
                fatigueResult.remainingHp(),
                turns
        );
    }

    public GroupBattleSimulation simulateGroupBossBattle(
            Hunt hunt,
            List<HuntParticipation> participations,
            Map<Long, GroupParticipantBattleContext> participantWeatherContexts
    ) {
        List<HuntParticipation> orderedParticipations = participations.stream()
                .sorted(Comparator.comparing(HuntParticipation::getJoinedAt).thenComparing(HuntParticipation::getId))
                .toList();

        Map<Long, HunterBattleState> hunterStates = new LinkedHashMap<>();
        for (HuntParticipation participation : orderedParticipations) {
            Hunter hunter = participation.getHunter();
            hunterStates.put(hunter.getId(), new HunterBattleState(
                    hunter.getId(),
                    hunter.getDisplayName(),
                    hunter.getLevel(),
                    hunter.getAppearance(),
                    hunter.getCurrentHp(),
                    hunter.isEndurancePotionActive()
            ));
        }

        int averageHunterLevel = Math.max(1, (int) Math.round(
                orderedParticipations.stream()
                        .mapToInt(participation -> participation.getHunter().getLevel())
                        .average()
                        .orElse(1)
        ));
        int bossHp = calculateScaledBossHp(hunt, averageHunterLevel, orderedParticipations.size());
        int initialBossHp = bossHp;
        String beastName = hunt.getBeasts().isEmpty()
                ? "Beast"
                : hunt.getBeasts().getFirst().getType().name();
        int turnNumber = 1;
        List<BattleTurnResponse> turns = new ArrayList<>();

        while (bossHp > 0 && hasLivingHunters(hunterStates)) {
            for (HuntParticipation participation : orderedParticipations) {
                HunterBattleState attacker = hunterStates.get(participation.getHunter().getId());
                if (attacker == null || !attacker.isAlive() || bossHp <= 0) {
                    continue;
                }

                GroupParticipantBattleContext attackerContext = participantWeatherContexts.get(attacker.hunterId());
                WeatherEffect attackerWeather = attackerContext != null
                        ? attackerContext.weatherEffect()
                        : WeatherEffect.neutral();

                int hunterDamage = rollHunterDamage(attacker.level(), attacker.appearance(), attackerWeather);
                bossHp = Math.max(0, bossHp - hunterDamage);
                turns.add(new BattleTurnResponse(
                        turnNumber++,
                        attacker.displayName(),
                        "hunter",
                        beastName,
                        "beast",
                        hunterDamage,
                        bossHp,
                        attacker.remainingHp(),
                        bossHp,
                        "%s takes %d damage".formatted(toDisplayName(beastName), hunterDamage),
                        formatGroupBattleState(hunterStates, bossHp)
                        ,
                        false,
                        false
                ));
            }

            if (bossHp <= 0 || !hasLivingHunters(hunterStates)) {
                break;
            }

            List<HunterBattleState> livingHunters = hunterStates.values().stream()
                    .filter(HunterBattleState::isAlive)
                    .toList();
            HunterBattleState target = livingHunters.get(ThreadLocalRandom.current().nextInt(livingHunters.size()));
            GroupParticipantBattleContext targetContext = participantWeatherContexts.get(target.hunterId());
            WeatherEffect targetWeather = targetContext != null
                    ? targetContext.weatherEffect()
                    : WeatherEffect.neutral();
            int bossDamage = rollBossDamage(averageHunterLevel, orderedParticipations.size());
            bossDamage = adjustBeastDamageAgainstHunter(
                    bossDamage,
                    hunt.getDifficulty(),
                    target.level(),
                    target.endurancePotionActive(),
                    target.appearance(),
                    targetWeather
            );
            target.applyDamage(bossDamage);
            turns.add(new BattleTurnResponse(
                    turnNumber++,
                    toDisplayName(beastName),
                    "beast",
                    target.displayName(),
                    "hunter",
                    bossDamage,
                    target.remainingHp(),
                    target.remainingHp(),
                    bossHp,
                    "%s takes %d damage".formatted(target.displayName(), bossDamage),
                    formatGroupBattleState(hunterStates, bossHp)
                    ,
                    false,
                    false
            ));
        }

        int turnNumberAfterBattle = turnNumber;
        for (HunterBattleState state : hunterStates.values()) {
            GroupParticipantBattleContext context = participantWeatherContexts.get(state.hunterId());
            WeatherEffect stateWeather = context != null
                    ? context.weatherEffect()
                    : WeatherEffect.neutral();
            if (stateWeather.enduranceCostMultiplier() > 1.0) {
                WeatherFatigueResult fatigueResult = applyWeatherFatigue(
                        stateWeather,
                        state.displayName(),
                        beastName,
                        turnNumberAfterBattle,
                        state.remainingHp(),
                        bossHp,
                        state.damageTaken(),
                        turns
                );
                turnNumberAfterBattle += fatigueResult.turnsAdded();
                if (fatigueResult.extraDamage() > 0) {
                    state.applyDamage(fatigueResult.extraDamage());
                }
            }
        }

        Map<Long, HunterBattleOutcome> outcomes = hunterStates.values().stream()
                .collect(LinkedHashMap::new, (result, state) -> result.put(
                        state.hunterId(),
                        new HunterBattleOutcome(state.remainingHp(), state.damageTaken())
                ), Map::putAll);

        return new GroupBattleSimulation(initialBossHp, bossHp <= 0, bossHp, turns, outcomes, participantWeatherContexts);
    }

    private String toDisplayName(String rawName) {
        return rawName.charAt(0) + rawName.substring(1).toLowerCase();
    }

    private int calculateScaledMonsterHp(Hunt hunt, int hunterLevel) {
        int baseHp = hunt.getBeasts().stream().mapToInt(beast -> beast.getHp()).sum();
        int hpGrowthPerLevel = switch (hunt.getDifficulty()) {
            case EASY -> 10;
            case MEDIUM -> 15;
            case HARD -> 22;
            case BOSS -> 35;
        };

        return baseHp + Math.max(0, hunterLevel - 1) * hpGrowthPerLevel;
    }

    private int calculateScaledBossHp(Hunt hunt, int averageHunterLevel, int partySize) {
        int baseHp = hunt.getBeasts().stream().mapToInt(beast -> beast.getHp()).sum();
        int levelScaling = Math.max(0, averageHunterLevel - 1) * 35;
        int partyScaling = Math.max(0, partySize - 1) * 60;
        return baseHp + 120 + levelScaling + partyScaling;
    }

    private int rollHunterDamage(int hunterLevel, Appearance appearance, WeatherEffect weatherEffect) {
        int minDamage = 8 + (hunterLevel * 2);
        int maxDamage = 14 + (hunterLevel * 3);
        if (appearance == Appearance.HUNTER) {
            maxDamage += 4;
        }
        if (appearance == Appearance.RANGER) {
            int midpoint = minDamage + Math.floorDiv(maxDamage - minDamage, 2);
            if (ThreadLocalRandom.current().nextInt(100) < 65) {
                return rollBetween(midpoint, maxDamage);
            }
        }
        int rolledDamage = rollBetween(minDamage, maxDamage);
        rolledDamage = Math.max(1, Math.round(rolledDamage * (float) weatherEffect.getHunterAttackRollMultiplier(appearance)));
        return Math.max(1, Math.round(rolledDamage / (float) weatherEffect.beastResistanceMultiplier()));
    }

    private int rollMonsterDamage(Hunt hunt, int hunterLevel, WeatherEffect weatherEffect) {
        int baseDamage = switch (hunt.getDifficulty()) {
            case EASY -> 6 + hunterLevel;
            case MEDIUM -> 10 + hunterLevel;
            case HARD -> 14 + (hunterLevel * 2);
            case BOSS -> 20 + (hunterLevel * 2);
        };

        int rolledDamage = rollBetween(Math.max(1, baseDamage - 2), baseDamage + 3);
        return applyWeatherOutgoingDamage(rolledDamage, hunt.getDifficulty(), weatherEffect);
    }

    private int rollBossDamage(int averageHunterLevel, int partySize) {
        int baseDamage = 14 + (averageHunterLevel * 2) + partySize;
        return rollBetween(Math.max(1, baseDamage - 2), baseDamage + 4);
    }

    private int calculateEnduranceReduction(int hunterLevel) {
        return 5 + Math.floorDiv(hunterLevel, 2);
    }

    private int applyPassiveDamageReduction(int damage, Appearance appearance) {
        if (appearance == Appearance.KNIGHT) {
            return Math.max(1, damage - 2);
        }
        return damage;
    }

    int adjustBeastDamageAgainstHunter(
            int rolledDamage,
            Difficulty difficulty,
            int hunterLevel,
            boolean endurancePotionActive,
            Appearance appearance,
            WeatherEffect weatherEffect
    ) {
        int adjustedDamage = applyWeatherOutgoingDamage(rolledDamage, difficulty, weatherEffect);
        if (endurancePotionActive) {
            adjustedDamage = Math.max(1, adjustedDamage - calculateEnduranceReduction(hunterLevel));
        }
        adjustedDamage = applyPassiveDamageReduction(adjustedDamage, appearance);
        return applyWeatherIncomingDamage(adjustedDamage, weatherEffect);
    }

    private boolean hasLivingHunters(Map<Long, HunterBattleState> hunterStates) {
        return hunterStates.values().stream().anyMatch(HunterBattleState::isAlive);
    }

    private String formatGroupBattleState(Map<Long, HunterBattleState> hunterStates, int bossHp) {
        String partyState = hunterStates.values().stream()
                .map(state -> "%s: %d HP%s".formatted(
                        state.displayName(),
                        state.remainingHp(),
                        state.isAlive() ? "" : " (down)"
                ))
                .reduce((left, right) -> left + ", " + right)
                .orElse("No hunters");
        return "%s | Boss HP: %d".formatted(partyState, bossHp);
    }

    private int rollBetween(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private int applyWeatherOutgoingDamage(int damage, Difficulty difficulty, WeatherEffect weatherEffect) {
        return Math.max(1, Math.round(damage * (float) weatherEffect.getBeastDamageMultiplier(difficulty)));
    }

    private int applyWeatherIncomingDamage(int damage, WeatherEffect weatherEffect) {
        return Math.max(1, Math.round(damage * (float) weatherEffect.hunterDamageTakenMultiplier()));
    }

    private WeatherFatigueResult applyWeatherFatigue(
            WeatherEffect weatherEffect,
            String hunterName,
            String beastName,
            int turnNumber,
            int hunterHp,
            int beastHp,
            int damageTaken,
            List<BattleTurnResponse> turns
    ) {
        if (weatherEffect.enduranceCostMultiplier() <= 1.0 || damageTaken <= 0 || hunterHp <= 0) {
            return new WeatherFatigueResult(hunterHp, damageTaken, 0, 0);
        }

        int fatigueDamage = Math.max(
                1,
                Math.round(damageTaken * (float) (weatherEffect.enduranceCostMultiplier() - 1.0))
        );
        int newHunterHp = Math.max(0, hunterHp - fatigueDamage);
        int newTotalDamageTaken = damageTaken + fatigueDamage;
        turns.add(new BattleTurnResponse(
                turnNumber,
                weatherEffect.displayName(),
                "weather",
                hunterName,
                "hunter",
                fatigueDamage,
                newHunterHp,
                newHunterHp,
                beastHp,
                "%s drains %d extra HP from %s".formatted(weatherEffect.displayName(), fatigueDamage, hunterName),
                "Hunter HP: %d, Boss HP: %d".formatted(newHunterHp, beastHp),
                false,
                false
        ));
        return new WeatherFatigueResult(newHunterHp, newTotalDamageTaken, fatigueDamage, 1);
    }

    private record WeatherFatigueResult(
            int remainingHp,
            int totalDamageTaken,
            int extraDamage,
            int turnsAdded
    ) {
    }

    private static final class HunterBattleState {
        private final Long hunterId;
        private final String displayName;
        private final int level;
        private final Appearance appearance;
        private final boolean endurancePotionActive;
        private int remainingHp;
        private int damageTaken;

        private HunterBattleState(
                Long hunterId,
                String displayName,
                int level,
                Appearance appearance,
                int remainingHp,
                boolean endurancePotionActive
        ) {
            this.hunterId = hunterId;
            this.displayName = displayName;
            this.level = level;
            this.appearance = appearance;
            this.remainingHp = remainingHp;
            this.endurancePotionActive = endurancePotionActive;
        }

        private void applyDamage(int damage) {
            remainingHp = Math.max(0, remainingHp - damage);
            damageTaken += damage;
        }

        private boolean isAlive() {
            return remainingHp > 0;
        }

        private Long hunterId() {
            return hunterId;
        }

        private String displayName() {
            return displayName;
        }

        private int level() {
            return level;
        }

        private Appearance appearance() {
            return appearance;
        }

        private int remainingHp() {
            return remainingHp;
        }

        private int damageTaken() {
            return damageTaken;
        }

        private boolean endurancePotionActive() {
            return endurancePotionActive;
        }
    }
}
