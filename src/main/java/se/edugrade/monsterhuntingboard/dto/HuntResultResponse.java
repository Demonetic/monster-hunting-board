package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.Hunter;

public record HuntResultResponse(
        Long huntId,
        String huntTitle,
        boolean won,
        int expChange,
        int goldChange,
        int newExp,
        int newGold,
        int newLevel,
        int newBaseHp
) {
    public static HuntResultResponse from(
            Hunt hunt,
            Hunter hunter,
            boolean won,
            int expChange,
            int goldChange
    ) {
        return new HuntResultResponse(
                hunt.getId(),
                hunt.getTitle(),
                won,
                expChange,
                goldChange,
                hunter.getExp(),
                hunter.getGold(),
                hunter.getLevel(),
                hunter.getBaseHp()
        );
    }
}
