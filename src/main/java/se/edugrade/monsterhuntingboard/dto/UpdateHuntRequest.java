package se.edugrade.monsterhuntingboard.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.HuntStatus;

public record UpdateHuntRequest(
        @Size(min = 3, max = 80) String title,
        Difficulty difficulty,
        HuntStatus status,
        LocalDateTime startTime,
        @Min(1) Integer maxPartySize,
        List<Long> beastIds,
        @Min(0) Integer rewardExp,
        @Min(0) Integer rewardGold
) {
}
