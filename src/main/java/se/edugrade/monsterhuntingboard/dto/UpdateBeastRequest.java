package se.edugrade.monsterhuntingboard.dto;

import jakarta.validation.constraints.Min;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;

public record UpdateBeastRequest(
        BeastType type,
        Difficulty difficulty,
        @Min(1) Integer hp,
        @Min(1) Integer attackPower,
        @Min(0) Integer rewardExp,
        @Min(0) Integer rewardGold
) {
}
