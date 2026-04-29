package se.edugrade.monsterhuntingboard.dto;

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
}
