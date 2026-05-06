package se.edugrade.monsterhuntingboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;
import se.edugrade.monsterhuntingboard.repository.UserAccountRepository;
import se.edugrade.monsterhuntingboard.util.GameBalanceUtil;

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

        assertThat(response.token()).isNotBlank();
        assertThat(response.username()).isEqualTo(username);
        assertThat(response.role()).isEqualTo(Role.HUNTER);
        assertThat(userAccountRepository.existsByUsername(username)).isTrue();
        Hunter savedHunter = userAccountRepository.findByUsername(username).orElseThrow().getHunter();
        assertThat(savedHunter.getBaseHp()).isEqualTo(100);
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

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void loginWorksAndBardRegistrationIsAllowed() {
        String username = "login-" + TestIds.shortId();
        saveHunterUser(username, "Login Hunter", Appearance.RANGER);

        AuthResponse response = authService.login(new LoginRequest(username, "password123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.username()).isEqualTo(username);
        assertThat(response.role()).isEqualTo(Role.HUNTER);

        RegisterRequest bardRequest = new RegisterRequest(
                "bard-" + TestIds.shortId(),
                "password123",
                "Bard Hunter",
                Appearance.BARD
        );
        AuthResponse bardResponse = authService.register(bardRequest);
        assertThat(bardResponse.username()).startsWith("bard-");
    }

    @Test
    void paladinRegistrationGetsHigherBaseHp() {
        String username = "pala-" + TestIds.shortId();

        authService.register(new RegisterRequest(
                username,
                "password123",
                "Paladin Hunter",
                Appearance.PALADIN
        ));

        Hunter hunter = userAccountRepository.findByUsername(username).orElseThrow().getHunter();
        assertThat(hunter.getBaseHp()).isEqualTo(GameBalanceUtil.calculateBaseHp(1, Appearance.PALADIN));
        assertThat(hunter.getCurrentHp()).isEqualTo(GameBalanceUtil.calculateBaseHp(1, Appearance.PALADIN));
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
                .baseHp(GameBalanceUtil.calculateBaseHp(1, appearance))
                .currentHp(GameBalanceUtil.calculateBaseHp(1, appearance))
                .userAccount(userAccount)
                .build();

        userAccount.setHunter(hunter);
        hunter.setUserAccount(userAccount);
        userAccountRepository.save(userAccount);
    }
}
