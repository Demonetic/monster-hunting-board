package se.edugrade.monsterhuntingboard.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;

public record CreateHuntRequest(
        @NotBlank
        @Size(min = 3, max = 80)
        String title,

        @NotNull
        HuntType type,

        @NotNull
        Difficulty difficulty,

        HuntStatus status,
        LocalDateTime startTime,
        Integer maxPartySize,

        @NotEmpty
        List<Long> beastIds,

        @Min(0)
        int rewardExp,

        @Min(0)
        int rewardGold
) {
}
