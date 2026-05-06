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
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntParticipation;
import se.edugrade.monsterhuntingboard.model.Hunter;

@Service
public class BattleService {
    private static final Logger log = LoggerFactory.getLogger(BattleService.class);

    public boolean rollWin() {
        boolean won = Math.random() < 0.7;
        log.debug("Battle roll result: {}", won ? "WIN" : "LOSS");
        return won;
    }

    public int calculateDamageTaken(Hunt hunt, boolean won, boolean endurancePotionActive) {
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

        log.debug("Calculated damage taken for hunt {}: {}", hunt.getId(), scaledDamage);
        return scaledDamage;
    }

    public SoloBattleSimulation simulateSoloBattle(Hunt hunt, Hunter hunter) {
        int hunterHp = hunter.getCurrentHp();
        int monsterHp = calculateScaledMonsterHp(hunt, hunter.getLevel());
        boolean hunterTurn = ThreadLocalRandom.current().nextBoolean();
        int turnNumber = 1;
        int totalDamageTaken = 0;
        List<BattleTurnResponse> turns = new ArrayList<>();

        while (hunterHp > 0 && monsterHp > 0) {
            if (hunterTurn) {
                int hunterDamage = rollHunterDamage(hunter.getLevel());
                monsterHp = Math.max(0, monsterHp - hunterDamage);
                turns.add(new BattleTurnResponse(
                        turnNumber++,
                        hunter.getDisplayName(),
                        "Boss",
                        hunterDamage,
                        "Hunter HP: %d, Boss HP: %d".formatted(hunterHp, monsterHp)
                ));
            } else {
                int monsterDamage = rollMonsterDamage(hunt, hunter.getLevel());
                if (hunter.isEndurancePotionActive()) {
                    monsterDamage = Math.max(1, monsterDamage - calculateEnduranceReduction(hunter.getLevel()));
                }
                hunterHp = Math.max(0, hunterHp - monsterDamage);
                totalDamageTaken += monsterDamage;
                turns.add(new BattleTurnResponse(
                        turnNumber++,
                        "Boss",
                        hunter.getDisplayName(),
                        monsterDamage,
                        "Hunter HP: %d, Boss HP: %d".formatted(hunterHp, monsterHp)
                ));
            }

            hunterTurn = !hunterTurn;
        }

        return new SoloBattleSimulation(hunterHp > 0, totalDamageTaken, hunterHp, turns);
    }

    public GroupBattleSimulation simulateGroupBossBattle(Hunt hunt, List<HuntParticipation> participations) {
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
        int turnNumber = 1;
        List<BattleTurnResponse> turns = new ArrayList<>();

        while (bossHp > 0 && hasLivingHunters(hunterStates)) {
            for (HuntParticipation participation : orderedParticipations) {
                HunterBattleState attacker = hunterStates.get(participation.getHunter().getId());
                if (attacker == null || !attacker.isAlive() || bossHp <= 0) {
                    continue;
                }

                int hunterDamage = rollHunterDamage(attacker.level());
                bossHp = Math.max(0, bossHp - hunterDamage);
                turns.add(new BattleTurnResponse(
                        turnNumber++,
                        attacker.displayName(),
                        "Boss",
                        hunterDamage,
                        formatGroupBattleState(hunterStates, bossHp)
                ));
            }

            if (bossHp <= 0 || !hasLivingHunters(hunterStates)) {
                break;
            }

            List<HunterBattleState> livingHunters = hunterStates.values().stream()
                    .filter(HunterBattleState::isAlive)
                    .toList();
            HunterBattleState target = livingHunters.get(ThreadLocalRandom.current().nextInt(livingHunters.size()));
            int bossDamage = rollBossDamage(averageHunterLevel, orderedParticipations.size());
            if (target.endurancePotionActive()) {
                bossDamage = Math.max(1, bossDamage - calculateEnduranceReduction(target.level()));
            }
            target.applyDamage(bossDamage);
            turns.add(new BattleTurnResponse(
                    turnNumber++,
                    "Boss",
                    target.displayName(),
                    bossDamage,
                    formatGroupBattleState(hunterStates, bossHp)
            ));
        }

        Map<Long, HunterBattleOutcome> outcomes = hunterStates.values().stream()
                .collect(LinkedHashMap::new, (result, state) -> result.put(
                        state.hunterId(),
                        new HunterBattleOutcome(state.remainingHp(), state.damageTaken())
                ), Map::putAll);

        return new GroupBattleSimulation(bossHp <= 0, bossHp, turns, outcomes);
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

    private int rollHunterDamage(int hunterLevel) {
        int minDamage = 8 + (hunterLevel * 2);
        int maxDamage = 14 + (hunterLevel * 3);
        return rollBetween(minDamage, maxDamage);
    }

    private int rollMonsterDamage(Hunt hunt, int hunterLevel) {
        int baseDamage = switch (hunt.getDifficulty()) {
            case EASY -> 6 + hunterLevel;
            case MEDIUM -> 10 + hunterLevel;
            case HARD -> 14 + (hunterLevel * 2);
            case BOSS -> 20 + (hunterLevel * 2);
        };

        return rollBetween(Math.max(1, baseDamage - 2), baseDamage + 3);
    }

    private int rollBossDamage(int averageHunterLevel, int partySize) {
        int baseDamage = 14 + (averageHunterLevel * 2) + partySize;
        return rollBetween(Math.max(1, baseDamage - 2), baseDamage + 4);
    }

    private int calculateEnduranceReduction(int hunterLevel) {
        return 5 + Math.floorDiv(hunterLevel, 2);
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

    private static final class HunterBattleState {
        private final Long hunterId;
        private final String displayName;
        private final int level;
        private final boolean endurancePotionActive;
        private int remainingHp;
        private int damageTaken;

        private HunterBattleState(
                Long hunterId,
                String displayName,
                int level,
                int remainingHp,
                boolean endurancePotionActive
        ) {
            this.hunterId = hunterId;
            this.displayName = displayName;
            this.level = level;
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
