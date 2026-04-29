package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;

public record BeastResponse(
        Long id,
        BeastType type,
        Difficulty difficulty,
        int hp,
        int attackPower,
        int rewardExp,
        int rewardGold
) {
}
