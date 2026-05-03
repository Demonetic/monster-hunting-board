package se.edugrade.monsterhuntingboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import se.edugrade.monsterhuntingboard.util.TestIds;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;

@DataJpaTest
@ActiveProfiles("test")
class HunterRepositoryTest {

    @Autowired
    private HunterRepository hunterRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private String username;
    private String displayName;

    @BeforeEach
    void setUp() {
        username = "hunt-" + TestIds.shortId();
        displayName = "Aria-" + TestIds.shortId();

        UserAccount userAccount = UserAccount.builder()
                .username(username)
                .password("password123")
                .role(Role.HUNTER)
                .build();

        Hunter hunter = Hunter.builder()
                .displayName(displayName)
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
    void findByUserAccountUsernameAndDisplayNameWork() {
        assertThat(hunterRepository.findByUserAccountUsername(username)).isPresent();
        assertThat(hunterRepository.existsByDisplayName(displayName)).isTrue();
    }
}
