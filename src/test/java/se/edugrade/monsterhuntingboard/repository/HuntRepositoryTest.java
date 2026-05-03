package se.edugrade.monsterhuntingboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;

@DataJpaTest
@ActiveProfiles("test")
class HuntRepositoryTest {

    @Autowired
    private HuntRepository huntRepository;

    @Autowired
    private BeastRepository beastRepository;

    private Beast beast;

    @BeforeEach
    void setUp() {
        beast = beastRepository.save(Beast.builder()
                .type(BeastType.BASILISK)
                .difficulty(Difficulty.EASY)
                .hp(100)
                .attackPower(20)
                .rewardExp(50)
                .rewardGold(25)
                .build());

        huntRepository.save(Hunt.builder()
                .title("Scheduled Hunt")
                .type(HuntType.HUNT)
                .difficulty(Difficulty.MEDIUM)
                .status(HuntStatus.SCHEDULED)
                .startTime(LocalDateTime.now().plusHours(1))
                .maxPartySize(4)
                .beasts(List.of(beast))
                .rewardExp(100)
                .rewardGold(75)
                .build());
    }

    @Test
    void statusAndBeastLookupMethodsWork() {
        assertThat(huntRepository.findByStatus(HuntStatus.SCHEDULED)).hasSize(1);
        assertThat(huntRepository.existsByBeastsId(beast.getId())).isTrue();
    }
}
