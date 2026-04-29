package se.edugrade.monsterhuntingboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import se.edugrade.monsterhuntingboard.model.Appearance;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        String username,

        @NotBlank
        @Size(min = 6, max = 100)
        String password,

        @NotBlank
        @Size(min = 2, max = 40)
        String displayName,

        @NotNull
        Appearance appearance
) {
}
