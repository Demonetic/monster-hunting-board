package se.edugrade.monsterhuntingboard.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import se.edugrade.monsterhuntingboard.model.BeastType;

public record UpdateBeastRequest(
        @Size(max = 80) String name,
        BeastType type,
        @Min(1) Integer hp,
        @Min(1) Integer attackPower,
        @Min(0) Integer rewardExp,
        @Min(0) Integer rewardGold
) {
}
