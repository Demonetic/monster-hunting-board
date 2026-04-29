package se.edugrade.monsterhuntingboard.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import se.edugrade.monsterhuntingboard.util.TestIds;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;

@DataJpaTest
@ActiveProfiles("test")
class UserAccountRepositoryTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    private String username;

    @BeforeEach
    void setUp() {
        username = "user-" + TestIds.shortId();
        userAccountRepository.save(UserAccount.builder()
                .username(username)
                .password("password123")
                .role(Role.GAME_MASTER)
                .build());
    }

    @Test
    void usernameLookupMethodsWork() {
        assertTrue(userAccountRepository.findByUsername(username).isPresent());
        assertTrue(userAccountRepository.existsByUsername(username));
    }
}
