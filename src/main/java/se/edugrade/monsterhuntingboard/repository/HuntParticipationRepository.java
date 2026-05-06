package se.edugrade.monsterhuntingboard.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.monsterhuntingboard.model.HuntParticipation;

public interface HuntParticipationRepository extends JpaRepository<HuntParticipation, Long> {

    List<HuntParticipation> findByHunterId(Long hunterId);

    List<HuntParticipation> findByHuntId(Long huntId);

    List<HuntParticipation> findByHuntIdOrderByJoinedAtAscIdAsc(Long huntId);

    Optional<HuntParticipation> findByHunterIdAndHuntId(Long hunterId, Long huntId);

    boolean existsByHunterIdAndHuntId(Long hunterId, Long huntId);

    boolean existsByHuntId(Long huntId);

    long countByHuntId(Long huntId);
}
