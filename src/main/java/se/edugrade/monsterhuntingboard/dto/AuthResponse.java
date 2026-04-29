package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.Role;

public record AuthResponse(
        String token,
        String username,
        Role role
) {
}
