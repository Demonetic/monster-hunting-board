package se.edugrade.monsterhuntingboard.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.monsterhuntingboard.model.Hunter;

public interface HunterRepository extends JpaRepository<Hunter, Long> {

    Optional<Hunter> findByUserAccountUsername(String username);

    boolean existsByDisplayName(String displayName);
}
