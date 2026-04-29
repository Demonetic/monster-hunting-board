package se.edugrade.monsterhuntingboard.dto;

import jakarta.validation.constraints.NotNull;
import se.edugrade.monsterhuntingboard.model.Appearance;

public record UpdateAppearanceRequest(
        @NotNull
        Appearance appearance
) {
}
