package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Hunter;

public record HunterResponse(
        Long id,
        String displayName,
        Appearance appearance,
        int level,
        int exp,
        int gold,
        int baseHp,
        int currentHp
) {
    public static HunterResponse from(Hunter hunter) {
        return new HunterResponse(
                hunter.getId(),
                hunter.getDisplayName(),
                hunter.getAppearance(),
                hunter.getLevel(),
                hunter.getExp(),
                hunter.getGold(),
                hunter.getBaseHp(),
                hunter.getCurrentHp()
        );
    }
}
