package se.edugrade.monsterhuntingboard.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import se.edugrade.monsterhuntingboard.model.BeastType;

public record BeastRequest(
        @NotBlank
        @Size(max = 80)
        String name,

        @NotNull
        BeastType type,

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
