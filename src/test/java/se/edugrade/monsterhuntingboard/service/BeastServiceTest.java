package se.edugrade.monsterhuntingboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.BeastResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateBeastRequest;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.repository.BeastRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BeastServiceTest {

    @Autowired
    private BeastService beastService;

    @Autowired
    private BeastRepository beastRepository;

    @Autowired
    private HuntRepository huntRepository;

    private Beast freeBeast;
    private Beast usedBeast;

    @BeforeEach
    void setUp() {
        freeBeast = beastRepository.save(Beast.builder()
                .type(BeastType.BASILISK)
                .difficulty(Difficulty.EASY)
                .hp(100)
                .attackPower(20)
                .rewardExp(50)
                .rewardGold(25)
                .build());

        usedBeast = beastRepository.save(Beast.builder()
                .type(BeastType.GRIFFIN)
                .difficulty(Difficulty.MEDIUM)
                .hp(180)
                .attackPower(35)
                .rewardExp(100)
                .rewardGold(75)
                .build());

        huntRepository.save(Hunt.builder()
                .title("Used Beast Hunt")
                .type(HuntType.HUNT)
                .difficulty(Difficulty.MEDIUM)
                .status(HuntStatus.SCHEDULED)
                .startTime(LocalDateTime.now().plusHours(1))
                .maxPartySize(4)
                .beasts(List.of(usedBeast))
                .rewardExp(100)
                .rewardGold(75)
                .build());
    }

    @Test
    void updateBeastUpdatesOnlyFieldsProvided() {
        BeastResponse response = beastService.updateBeast(
                freeBeast.getId(),
                new UpdateBeastRequest(null, null, 250, 45, 130, null)
        );

        assertThat(response.hp()).isEqualTo(250);
        assertThat(response.attackPower()).isEqualTo(45);
        assertThat(response.rewardExp()).isEqualTo(130);
        assertThat(response.rewardGold()).isEqualTo(25);
        assertThat(response.type()).isEqualTo(BeastType.BASILISK);
    }

    @Test
    void deleteBeastAllowsUnusedButRejectsUsed() {
        beastService.deleteBeast(freeBeast.getId());
        assertThat(beastRepository.existsById(freeBeast.getId())).isFalse();
        assertThatThrownBy(() -> beastService.deleteBeast(usedBeast.getId()))
                .isInstanceOf(InvalidGameRuleException.class);
    }
}
