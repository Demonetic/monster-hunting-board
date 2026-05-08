package se.edugrade.monsterhuntingboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLocationRequest(
        @NotBlank
        @Size(max = 120)
        String city
) {
}
