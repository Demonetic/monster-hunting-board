package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Beast;

public record BeastResponse(
        Long id,
        String name,
        BeastType type,
        int hp,
        int attackPower,
        int rewardExp,
        int rewardGold
) {
    public static BeastResponse from(Beast beast) {
        return new BeastResponse(
                beast.getId(),
                beast.getName(),
                beast.getType(),
                beast.getHp(),
                beast.getAttackPower(),
                beast.getRewardExp(),
                beast.getRewardGold()
        );
    }
}
