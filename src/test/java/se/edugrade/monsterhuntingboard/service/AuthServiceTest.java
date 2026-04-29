package se.edugrade.monsterhuntingboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.util.TestIds;
import se.edugrade.monsterhuntingboard.dto.AuthResponse;
import se.edugrade.monsterhuntingboard.dto.LoginRequest;
import se.edugrade.monsterhuntingboard.dto.RegisterRequest;
import se.edugrade.monsterhuntingboard.exception.DuplicateResourceException;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;
import se.edugrade.monsterhuntingboard.repository.UserAccountRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerWorks() {
        String username = "newh-" + TestIds.shortId();
        RegisterRequest request = new RegisterRequest(
                username,
                "password123",
                "New Hunter",
                Appearance.KNIGHT
        );

        AuthResponse response = authService.register(request);

        assertNotNull(response.token());
        assertEquals(username, response.username());
        assertEquals(Role.HUNTER, response.role());
        assertTrue(userAccountRepository.existsByUsername(username));
    }

    @Test
    void duplicateUsernameThrowsDuplicateResourceException() {
        String username = "exist-" + TestIds.shortId();
        saveHunterUser(username, "Existing Hunter", Appearance.MAGE);

        RegisterRequest request = new RegisterRequest(
                username,
                "password123",
                "Duplicate Hunter",
                Appearance.PALADIN
        );

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void loginWorksAndBardRegistrationIsRejected() {
        String username = "login-" + TestIds.shortId();
        saveHunterUser(username, "Login Hunter", Appearance.RANGER);

        AuthResponse response = authService.login(new LoginRequest(username, "password123"));

        assertNotNull(response.token());
        assertEquals(username, response.username());
        assertEquals(Role.HUNTER, response.role());

        RegisterRequest bardRequest = new RegisterRequest(
                "bard-" + TestIds.shortId(),
                "password123",
                "Bard Hunter",
                Appearance.BARD
        );
        assertThrows(InvalidGameRuleException.class, () -> authService.register(bardRequest));
    }

    private void saveHunterUser(String username, String displayName, Appearance appearance) {
        UserAccount userAccount = UserAccount.builder()
                .username(username)
                .password(passwordEncoder.encode("password123"))
                .role(Role.HUNTER)
                .build();

        Hunter hunter = Hunter.builder()
                .displayName(displayName)
                .appearance(appearance)
                .level(1)
                .exp(0)
                .gold(0)
                .baseHp(100)
                .currentHp(100)
                .userAccount(userAccount)
                .build();

        userAccount.setHunter(hunter);
        hunter.setUserAccount(userAccount);
        userAccountRepository.save(userAccount);
    }
}
