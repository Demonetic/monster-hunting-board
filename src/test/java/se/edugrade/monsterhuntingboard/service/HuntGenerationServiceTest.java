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
                .filter(hunt -> hunt.getSourceType() == HuntSourceType.DAILY_BOUNTY || hunt.getSourceType() == HuntSourceType.WEEKLY_CONTRACT)
                .allMatch(hunt -> hunt.getBeasts().size() == 1 && hunt.getTitle().contains(hunt.getBeasts().getFirst().getName())))
                .isTrue();
        assertThat(huntRepository.findAll().stream()
                .filter(hunt -> hunt.getSourceType() == HuntSourceType.DAILY_BOSS)
                .allMatch(hunt -> hunt.getRoomOpensAt() != null && hunt.getStartTime() != null && hunt.getRoomOpensAt().isEqual(hunt.getStartTime().minusMinutes(10))))
                .isTrue();
    }

    private void seedBeasts() {
        beastRepository.saveAll(List.of(
                Beast.builder().name("Basilisk").type(BeastType.BASILISK).hp(110).attackPower(18).rewardExp(60).rewardGold(30).build(),
                Beast.builder().name("Griffin").type(BeastType.GRIFFIN).hp(180).attackPower(32).rewardExp(110).rewardGold(70).build(),
                Beast.builder().name("Pegasus").type(BeastType.PEGASUS).hp(165).attackPower(28).rewardExp(100).rewardGold(60).build(),
                Beast.builder().name("Chimera").type(BeastType.CHIMERA).hp(260).attackPower(48).rewardExp(190).rewardGold(130).build(),
                Beast.builder().name("Phoenix").type(BeastType.PHOENIX).hp(320).attackPower(58).rewardExp(240).rewardGold(180).build(),
                Beast.builder().name("Dragon").type(BeastType.DRAGON).hp(520).attackPower(82).rewardExp(420).rewardGold(520).build()
        ));
    }
}
