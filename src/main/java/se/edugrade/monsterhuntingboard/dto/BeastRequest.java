package se.edugrade.monsterhuntingboard.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;

public record BeastRequest(
        @NotNull
        BeastType type,

        @NotNull
        Difficulty difficulty,

        @Min(1)
        int hp,

        @Min(1)
        int attackPower,

        @Min(0)
        int rewardExp,

        @Min(0)
        int rewardGold
) {
}
