package se.edugrade.monsterhuntingboard.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.monsterhuntingboard.model.HuntSourceType;
import se.edugrade.monsterhuntingboard.model.HuntTemplate;

public interface HuntTemplateRepository extends JpaRepository<HuntTemplate, Long> {

    List<HuntTemplate> findByActiveTrueAndSourceType(HuntSourceType sourceType);
}
