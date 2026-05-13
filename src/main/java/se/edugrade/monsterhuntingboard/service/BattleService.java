package se.edugrade.monsterhuntingboard.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
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
import se.edugrade.monsterhuntingboard.util.GameBalanceUtil;

@Service
public class BattleService {
    private static final Logger log = LoggerFactory.getLogger(BattleService.class);
    private static final String BEAST_COMBATANT_ID = "beast";
    private static final int HUNTER_MIN_DAMAGE_BASE = 9;
    private static final int HUNTER_MAX_DAMAGE_BASE = 15;
    private static final int HUNTER_MIN_DAMAGE_PER_LEVEL = 2;
    private static final int HUNTER_MAX_DAMAGE_PER_LEVEL = 3;
    private static final EnumMap<Difficulty, DifficultyBalance> DIFFICULTY_BALANCE = new EnumMap<>(Difficulty.class);

    static {
        DIFFICULTY_BALANCE.put(Difficulty.EASY, new DifficultyBalance(0.34f, 0.24f, 1));
        DIFFICULTY_BALANCE.put(Difficulty.MEDIUM, new DifficultyBalance(0.36f, 0.24f, 1));
        DIFFICULTY_BALANCE.put(Difficulty.HARD, new DifficultyBalance(0.24f, 0.26f, 2));
        DIFFICULTY_BALANCE.put(Difficulty.BOSS, new DifficultyBalance(0.24f, 0.28f, 2));
    }

    public boolean rollWin() {
        boolean won = Math.random() < 0.7;
        log.debug("Battle roll result: {}", won ? "WIN" : "LOSS");
        return won;
    }

    public int calculateDamageTaken(Hunt hunt, boolean won, boolean endurancePotionActive, WeatherEffect weatherEffect) {
        DifficultyBalance balance = getBalance(hunt.getDifficulty());
        int baseDamage = Math.max(1, Math.round(getAverageBeastAttackPower(hunt) * balance.beastAttackMultiplier()));
        int scaledDamage = won
                ? Math.max(4, Math.round(baseDamage * 0.95f))
                : Math.max(6, Math.round(baseDamage * 1.15f));

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
        String beastName = getPrimaryBeastName(hunt);
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
                        hunterCombatantId(hunter.getId()),
                        hunter.getDisplayName(),
                        "hunter",
                        BEAST_COMBATANT_ID,
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
                        BEAST_COMBATANT_ID,
                        toDisplayName(beastName),
                        "beast",
                        hunterCombatantId(hunter.getId()),
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
                hunterCombatantId(hunter.getId()),
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
        String beastName = getPrimaryBeastName(hunt);
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
                        hunterCombatantId(attacker.hunterId()),
                        attacker.displayName(),
                        "hunter",
                        BEAST_COMBATANT_ID,
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
                attacker.recordDamageDealt(hunterDamage);
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
            int bossDamage = rollBossDamage(hunt, averageHunterLevel, orderedParticipations.size());
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
                    BEAST_COMBATANT_ID,
                    toDisplayName(beastName),
                    "beast",
                    hunterCombatantId(target.hunterId()),
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
                        hunterCombatantId(state.hunterId()),
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
                        new HunterBattleOutcome(state.remainingHp(), state.damageTaken(), state.damageDealt())
                ), Map::putAll);

        return new GroupBattleSimulation(initialBossHp, bossHp <= 0, bossHp, turns, outcomes, participantWeatherContexts);
    }

    private String getPrimaryBeastName(Hunt hunt) {
        if (hunt.getBeasts().isEmpty()) {
            return "Unknown Beast";
        }

        String beastName = hunt.getBeasts().getFirst().getName();
        if (beastName != null && !beastName.isBlank()) {
            return beastName;
        }

        return formatBeastTypeName(hunt.getBeasts().getFirst().getType().name());
    }

    private String toDisplayName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "Unknown Beast";
        }

        return rawName.chars().allMatch(character -> character == '_' || Character.isUpperCase(character))
                ? formatBeastTypeName(rawName)
                : rawName;
    }

    private String formatBeastTypeName(String rawName) {
        String normalized = rawName.replace('_', ' ').toLowerCase();
        String[] words = normalized.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.isEmpty() ? "Unknown Beast" : builder.toString();
    }

    private int calculateScaledMonsterHp(Hunt hunt, int hunterLevel) {
        return GameBalanceUtil.calculateSoloBeastBattleHp(hunt.getDifficulty(), hunterLevel);
    }

    private int calculateScaledBossHp(Hunt hunt, int averageHunterLevel, int partySize) {
        return GameBalanceUtil.calculateBeastBattleHp(hunt.getDifficulty(), averageHunterLevel, partySize);
    }

    private int rollHunterDamage(int hunterLevel, Appearance appearance, WeatherEffect weatherEffect) {
        int minDamage = HUNTER_MIN_DAMAGE_BASE + (hunterLevel * HUNTER_MIN_DAMAGE_PER_LEVEL);
        int maxDamage = HUNTER_MAX_DAMAGE_BASE + (hunterLevel * HUNTER_MAX_DAMAGE_PER_LEVEL);
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
        DifficultyBalance balance = getBalance(hunt.getDifficulty());
        int baseDamage = Math.max(
                1,
                Math.round(
                        (getAverageBeastAttackPower(hunt) * balance.beastAttackMultiplier())
                                + (Math.max(0, hunterLevel - 1) * 0.6f)
                )
        );

        int rolledDamage = rollBetween(Math.max(1, baseDamage - 2), baseDamage + 3);
        return applyWeatherOutgoingDamage(rolledDamage, hunt.getDifficulty(), weatherEffect);
    }

    private int rollBossDamage(Hunt hunt, int averageHunterLevel, int partySize) {
        DifficultyBalance balance = getBalance(hunt.getDifficulty());
        int baseDamage = Math.max(
                1,
                Math.round(
                        (getAverageBeastAttackPower(hunt) * balance.bossAttackMultiplier())
                                + (Math.max(0, averageHunterLevel - 1) * 0.8f)
                ) + (Math.max(0, partySize - 1) * balance.bossAttackPerExtraHunter())
        );
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

    private int getAverageBeastAttackPower(Hunt hunt) {
        return Math.max(
                1,
                (int) Math.round(
                        hunt.getBeasts().stream()
                                .mapToInt(beast -> beast.getAttackPower())
                                .average()
                                .orElse(1)
                )
        );
    }

    private DifficultyBalance getBalance(Difficulty difficulty) {
        return DIFFICULTY_BALANCE.get(difficulty);
    }

    private int applyWeatherOutgoingDamage(int damage, Difficulty difficulty, WeatherEffect weatherEffect) {
        return Math.max(1, Math.round(damage * (float) weatherEffect.getBeastDamageMultiplier(difficulty)));
    }

    private int applyWeatherIncomingDamage(int damage, WeatherEffect weatherEffect) {
        return Math.max(1, Math.round(damage * (float) weatherEffect.hunterDamageTakenMultiplier()));
    }

    private String hunterCombatantId(Long hunterId) {
        return "hunter-" + hunterId;
    }

    private WeatherFatigueResult applyWeatherFatigue(
            WeatherEffect weatherEffect,
            String hunterCombatantId,
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
                "weather",
                weatherEffect.displayName(),
                "weather",
                hunterCombatantId,
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

    private record DifficultyBalance(
            float beastAttackMultiplier,
            float bossAttackMultiplier,
            int bossAttackPerExtraHunter
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
        private int damageDealt;

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

        private int damageDealt() {
            return damageDealt;
        }

        private void recordDamageDealt(int damage) {
            damageDealt += Math.max(0, damage);
        }

        private boolean endurancePotionActive() {
            return endurancePotionActive;
        }
    }
}
