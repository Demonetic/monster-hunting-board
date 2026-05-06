package se.edugrade.monsterhuntingboard.dto;

import java.time.LocalDateTime;
import se.edugrade.monsterhuntingboard.model.HunterInventoryItem;
import se.edugrade.monsterhuntingboard.model.InventoryItemType;

public record InventoryItemResponse(
        Long id,
        int slotIndex,
        InventoryItemType itemType,
        String displayName,
        String description,
        int price,
        LocalDateTime acquiredAt
) {
    public static InventoryItemResponse from(HunterInventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getSlotIndex(),
                item.getItemType(),
                item.getItemType().getDisplayName(),
                item.getItemType().getDescription(),
                item.getItemType().getPrice(),
                item.getCreatedAt()
        );
    }
}
