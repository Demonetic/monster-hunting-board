package se.edugrade.monsterhuntingboard.dto;

import java.util.List;
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
        int newBaseHp,
        int newCurrentHp,
        int damageTaken,
        boolean expPotionApplied,
        boolean endurancePotionApplied,
        List<BattleTurnResponse> turns
) {
    public static HuntResultResponse from(
            Hunt hunt,
            Hunter hunter,
            boolean won,
            int expChange,
            int goldChange,
            int damageTaken,
            boolean expPotionApplied,
            boolean endurancePotionApplied,
            List<BattleTurnResponse> turns
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
                hunter.getBaseHp(),
                hunter.getCurrentHp(),
                damageTaken,
                expPotionApplied,
                endurancePotionApplied,
                turns
        );
    }
}
