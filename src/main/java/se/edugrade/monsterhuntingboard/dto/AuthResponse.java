package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;

public record AuthResponse(
        String token,
        String username,
        Role role
) {
    public static AuthResponse from(String token, UserAccount userAccount) {
        return new AuthResponse(token, userAccount.getUsername(), userAccount.getRole());
    }
}
