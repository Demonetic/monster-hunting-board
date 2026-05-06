package se.edugrade.monsterhuntingboard.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.edugrade.monsterhuntingboard.dto.BattleTurnResponse;
import se.edugrade.monsterhuntingboard.model.Hunt;
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
                turns.add(new BattleTurnResponse(turnNumber++, "HUNTER", hunterDamage, hunterHp, monsterHp));
            } else {
                int monsterDamage = rollMonsterDamage(hunt, hunter.getLevel());
                if (hunter.isEndurancePotionActive()) {
                    monsterDamage = Math.max(1, monsterDamage - calculateEnduranceReduction(hunter.getLevel()));
                }
                hunterHp = Math.max(0, hunterHp - monsterDamage);
                totalDamageTaken += monsterDamage;
                turns.add(new BattleTurnResponse(turnNumber++, "MONSTER", monsterDamage, hunterHp, monsterHp));
            }

            hunterTurn = !hunterTurn;
        }

        return new SoloBattleSimulation(hunterHp > 0, totalDamageTaken, hunterHp, turns);
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

    private int calculateEnduranceReduction(int hunterLevel) {
        return 5 + Math.floorDiv(hunterLevel, 2);
    }

    private int rollBetween(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }
}
