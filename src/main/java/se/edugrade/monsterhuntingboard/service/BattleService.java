package se.edugrade.monsterhuntingboard.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;

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
                .mapToInt(Beast::getAttackPower)
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
}
