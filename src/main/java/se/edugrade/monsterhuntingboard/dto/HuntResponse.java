package se.edugrade.monsterhuntingboard.dto;

import java.time.LocalDateTime;
import java.util.List;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;

public record HuntResponse(
        Long id,
        String title,
        HuntType type,
        Difficulty difficulty,
        HuntStatus status,
        LocalDateTime startTime,
        Integer maxPartySize,
        List<BeastResponse> beasts,
        int currentPartySize,
        int rewardExp,
        int rewardGold,
        LocalDateTime createdAt
) {
}
