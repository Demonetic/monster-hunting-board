package se.edugrade.monsterhuntingboard.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;

public interface HuntRepository extends JpaRepository<Hunt, Long> {

    List<Hunt> findByType(HuntType type);

    List<Hunt> findByStatus(HuntStatus status);

    List<Hunt> findByDifficulty(Difficulty difficulty);

    List<Hunt> findByStartTimeAfter(LocalDateTime startTime);

    List<Hunt> findByStatusAndStartTimeAfter(HuntStatus status, LocalDateTime startTime);

    List<Hunt> findByTypeAndStatus(HuntType type, HuntStatus status);

    boolean existsByBeastsId(Long beastId);
}
