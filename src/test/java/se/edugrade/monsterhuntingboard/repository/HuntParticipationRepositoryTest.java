package se.edugrade.monsterhuntingboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import se.edugrade.monsterhuntingboard.util.TestIds;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntParticipation;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;

@DataJpaTest
@ActiveProfiles("test")
class HuntParticipationRepositoryTest {

    @Autowired
    private HuntParticipationRepository huntParticipationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private HuntRepository huntRepository;

    @Autowired
    private BeastRepository beastRepository;

    private Hunter hunter;
    private Hunt hunt;

    @BeforeEach
    void setUp() {
        UserAccount userAccount = UserAccount.builder()
                .username("hunt-" + TestIds.shortId())
                .password("password123")
                .role(Role.HUNTER)
                .build();

        hunter = Hunter.builder()
                .displayName("Aria-" + TestIds.shortId())
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

        Beast beast = beastRepository.save(Beast.builder()
                .name("Griffin")
                .type(BeastType.GRIFFIN)
                .hp(180)
                .attackPower(35)
                .rewardExp(100)
                .rewardGold(75)
                .build());

        hunt = huntRepository.save(Hunt.builder()
                .title("Participation Hunt")
                .type(HuntType.HUNT)
                .difficulty(Difficulty.MEDIUM)
                .status(HuntStatus.SCHEDULED)
                .startTime(LocalDateTime.now().plusHours(1))
                .maxPartySize(4)
                .beasts(List.of(beast))
                .rewardExp(100)
                .rewardGold(75)
                .build());

        huntParticipationRepository.save(HuntParticipation.builder()
                .hunter(hunter)
                .hunt(hunt)
                .completed(false)
                .won(false)
                .expChange(0)
                .goldChange(0)
                .build());
    }

    @Test
    void findByHunterIdAndHuntIdReturnsParticipation() {
        assertThat(huntParticipationRepository.findByHunterIdAndHuntId(hunter.getId(), hunt.getId())).isPresent();
        assertThat(huntParticipationRepository.existsByHunterIdAndHuntId(hunter.getId(), hunt.getId())).isTrue();
    }

    @Test
    void countByHuntIdReturnsParticipantCount() {
        assertThat(huntParticipationRepository.countByHuntId(hunt.getId())).isEqualTo(1L);
    }
}
