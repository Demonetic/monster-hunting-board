package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.Appearance;

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
}
