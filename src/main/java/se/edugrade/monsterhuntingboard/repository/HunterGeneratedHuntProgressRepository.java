package se.edugrade.monsterhuntingboard.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.monsterhuntingboard.model.HunterGeneratedHuntProgress;

public interface HunterGeneratedHuntProgressRepository extends JpaRepository<HunterGeneratedHuntProgress, Long> {

    Optional<HunterGeneratedHuntProgress> findByHunterIdAndHuntId(Long hunterId, Long huntId);
}
