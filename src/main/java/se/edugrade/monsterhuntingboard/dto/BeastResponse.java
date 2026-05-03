package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Beast;
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
    public static BeastResponse from(Beast beast) {
        return new BeastResponse(
                beast.getId(),
                beast.getType(),
                beast.getDifficulty(),
                beast.getHp(),
                beast.getAttackPower(),
                beast.getRewardExp(),
                beast.getRewardGold()
        );
    }
}
