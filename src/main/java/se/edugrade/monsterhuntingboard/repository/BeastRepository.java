package se.edugrade.monsterhuntingboard.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;

public interface BeastRepository extends JpaRepository<Beast, Long> {

    List<Beast> findByType(BeastType type);

    default Optional<Beast> findFirstByType(BeastType type) {
        return findByType(type).stream().findFirst();
    }
}
