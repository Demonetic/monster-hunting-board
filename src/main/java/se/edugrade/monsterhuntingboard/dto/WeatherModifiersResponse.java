package se.edugrade.monsterhuntingboard.dto;

import java.util.Map;
import java.util.stream.Collectors;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.WeatherEffect;

public record WeatherModifiersResponse(
        double rewardMultiplier,
        double expMultiplier,
        double goldMultiplier,
        double defeatPenaltyMultiplier,
        double hunterDamageTakenMultiplier,
        double enduranceCostMultiplier,
        double beastResistanceMultiplier,
        double beastDamageMultiplier,
        double enemyDamageMultiplier,
        double attackRollMultiplier,
        double defaultHunterAttackRollMultiplier,
        Map<String, Double> hunterAttackRollMultipliers,
        Map<String, Double> beastDamageMultipliersByDifficulty
) {
    public static WeatherModifiersResponse from(WeatherEffect effect) {
        return new WeatherModifiersResponse(
                effect.rewardMultiplier(),
                effect.expMultiplier(),
                effect.goldMultiplier(),
                effect.defeatPenaltyMultiplier(),
                effect.hunterDamageTakenMultiplier(),
                effect.enduranceCostMultiplier(),
                effect.beastResistanceMultiplier(),
                effect.beastDamageMultiplier(),
                effect.enemyDamageMultiplier(),
                effect.attackRollMultiplier(),
                effect.defaultHunterAttackRollMultiplier(),
                effect.hunterAttackRollMultipliers().entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue)),
                effect.beastDamageMultipliersByDifficulty().entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue))
        );
    }
}
