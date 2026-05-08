package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.Appearance;

public record AppearanceOptionResponse(
        String appearance,
        String displayName,
        String passiveSkillName,
        String passiveSkillDescription
) {
    public static AppearanceOptionResponse from(Appearance appearance) {
        return new AppearanceOptionResponse(
                appearance.name(),
                appearance.getDisplayName(),
                appearance.getPassiveSkillName(),
                appearance.getPassiveSkillDescription()
        );
    }
}
