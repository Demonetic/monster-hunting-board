package se.edugrade.monsterhuntingboard.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.monsterhuntingboard.model.HunterInventoryItem;

public interface HunterInventoryItemRepository extends JpaRepository<HunterInventoryItem, Long> {
    List<HunterInventoryItem> findByHunterIdOrderBySlotIndexAsc(Long hunterId);
}
