package se.edugrade.monsterhuntingboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.util.TestIds;
import se.edugrade.monsterhuntingboard.dto.HunterResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateAppearanceRequest;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;
import se.edugrade.monsterhuntingboard.repository.UserAccountRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HunterServiceTest {

    @Autowired
    private HunterService hunterService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String username;

    @BeforeEach
    void setUp() {
        username = "hunt-" + TestIds.shortId();

        UserAccount userAccount = UserAccount.builder()
                .username(username)
                .password(passwordEncoder.encode("password123"))
                .role(Role.HUNTER)
                .build();

        Hunter hunter = Hunter.builder()
                .displayName("Aria")
                .appearance(Appearance.MAGE)
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

    @Test
    void getCurrentHunterWorks() {
        HunterResponse response = hunterService.getCurrentHunter(username);

        assertEquals("Aria", response.displayName());
        assertEquals(Appearance.MAGE, response.appearance());
        assertEquals(1, response.level());
    }

    @Test
    void updateAppearanceWorksAndRejectsBard() {
        HunterResponse response = hunterService.updateAppearance(
                username,
                new UpdateAppearanceRequest(Appearance.PALADIN)
        );

        assertEquals(Appearance.PALADIN, response.appearance());
        assertThrows(
                InvalidGameRuleException.class,
                () -> hunterService.updateAppearance(username, new UpdateAppearanceRequest(Appearance.BARD))
        );
    }
}
