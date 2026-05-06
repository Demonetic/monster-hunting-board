package se.edugrade.monsterhuntingboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.HuntSourceType;
import se.edugrade.monsterhuntingboard.repository.BeastRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;
import se.edugrade.monsterhuntingboard.repository.HuntTemplateRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HuntGenerationServiceTest {

    @Autowired
    private HuntGenerationService huntGenerationService;

    @Autowired
    private BeastRepository beastRepository;

    @Autowired
    private HuntRepository huntRepository;

    @Autowired
    private HuntTemplateRepository huntTemplateRepository;

    @Test
    void generateCurrentRotationsCreatesExpectedHuntCounts() {
        seedBeasts();

        huntGenerationService.generateCurrentRotations();

        assertThat(huntTemplateRepository.findByActiveTrueAndSourceType(HuntSourceType.REPEATABLE)).hasSizeGreaterThanOrEqualTo(3);
        assertThat(huntRepository.findAll().stream().filter(hunt -> hunt.getSourceType() == HuntSourceType.REPEATABLE)).hasSize(3);
        assertThat(huntRepository.findAll().stream().filter(hunt -> hunt.getSourceType() == HuntSourceType.DAILY_BOUNTY)).hasSize(2);
        assertThat(huntRepository.findAll().stream().filter(hunt -> hunt.getSourceType() == HuntSourceType.WEEKLY_CONTRACT)).hasSize(3);
        assertThat(huntRepository.findAll().stream().filter(hunt -> hunt.getSourceType() == HuntSourceType.DAILY_BOSS)).hasSize(3);
        assertThat(huntRepository.findAll().stream()
                .filter(hunt -> hunt.getSourceType() == HuntSourceType.DAILY_BOSS)
                .allMatch(hunt -> hunt.getRoomOpensAt() != null && hunt.getStartTime() != null && hunt.getRoomOpensAt().isEqual(hunt.getStartTime().minusMinutes(10))))
                .isTrue();
    }

    private void seedBeasts() {
        beastRepository.saveAll(List.of(
                Beast.builder().type(BeastType.BASILISK).difficulty(Difficulty.EASY).hp(110).attackPower(18).rewardExp(60).rewardGold(30).build(),
                Beast.builder().type(BeastType.GRIFFIN).difficulty(Difficulty.MEDIUM).hp(180).attackPower(32).rewardExp(110).rewardGold(70).build(),
                Beast.builder().type(BeastType.CHIMERA).difficulty(Difficulty.HARD).hp(260).attackPower(48).rewardExp(190).rewardGold(130).build(),
                Beast.builder().type(BeastType.PHOENIX).difficulty(Difficulty.HARD).hp(320).attackPower(58).rewardExp(240).rewardGold(180).build(),
                Beast.builder().type(BeastType.DRAGON).difficulty(Difficulty.BOSS).hp(520).attackPower(82).rewardExp(420).rewardGold(520).build()
        ));
    }
}
