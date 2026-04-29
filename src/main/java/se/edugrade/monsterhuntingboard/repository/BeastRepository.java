package se.edugrade.monsterhuntingboard.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;

public interface BeastRepository extends JpaRepository<Beast, Long> {

    List<Beast> findByDifficulty(Difficulty difficulty);

    List<Beast> findByType(BeastType type);

    List<Beast> findByDifficultyAndType(Difficulty difficulty, BeastType type);
}
