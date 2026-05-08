package se.edugrade.monsterhuntingboard.dto;

import java.util.Arrays;
import java.util.List;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;

public record AuthResponse(
        String token,
        String username,
        Role role,
        List<AppearanceOptionResponse> appearanceOptions
) {
    public static AuthResponse from(String token, UserAccount userAccount) {
        return new AuthResponse(
                token,
                userAccount.getUsername(),
                userAccount.getRole(),
                Arrays.stream(Appearance.values()).map(AppearanceOptionResponse::from).toList()
        );
    }
}
