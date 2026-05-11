package se.edugrade.monsterhuntingboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;

@DataJpaTest
@ActiveProfiles("test")
class BeastRepositoryTest {

    @Autowired
    private BeastRepository beastRepository;

    @BeforeEach
    void setUp() {
        beastRepository.save(Beast.builder()
                .name("Dragon")
                .type(BeastType.DRAGON)
                .hp(500)
                .attackPower(80)
                .rewardExp(400)
                .rewardGold(500)
                .build());
        beastRepository.save(Beast.builder()
                .name("Griffin")
                .type(BeastType.GRIFFIN)
                .hp(180)
                .attackPower(35)
                .rewardExp(100)
                .rewardGold(75)
                .build());
    }

    @Test
    void findByTypeReturnsMatchingBeasts() {
        assertThat(beastRepository.findByType(BeastType.GRIFFIN)).hasSize(1);
        assertThat(beastRepository.findFirstByType(BeastType.DRAGON)).isPresent();
    }
}
