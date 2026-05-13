package se.edugrade.monsterhuntingboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank(message = "message cannot be empty")
        @Size(max = 250, message = "message cannot be longer than 250 characters")
        String message
) {
}
