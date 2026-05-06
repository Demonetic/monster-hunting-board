package se.edugrade.monsterhuntingboard.dto;

import java.time.LocalDateTime;
import java.util.List;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntSourceType;
import se.edugrade.monsterhuntingboard.model.HuntType;

public record HuntResponse(
        Long id,
        String title,
        HuntType type,
        HuntSourceType sourceType,
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
    public static HuntResponse from(Hunt hunt, int currentPartySize) {
        return new HuntResponse(
                hunt.getId(),
                hunt.getTitle(),
                hunt.getType(),
                hunt.getSourceType(),
                hunt.getDifficulty(),
                hunt.getStatus(),
                hunt.getStartTime(),
                hunt.getMaxPartySize(),
                hunt.getBeasts().stream().map(BeastResponse::from).toList(),
                currentPartySize,
                hunt.getRewardExp(),
                hunt.getRewardGold(),
                hunt.getCreatedAt()
        );
    }
}
