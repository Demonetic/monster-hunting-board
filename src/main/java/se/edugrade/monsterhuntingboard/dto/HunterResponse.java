package se.edugrade.monsterhuntingboard.dto;

import java.util.List;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.HunterInventoryItem;
import se.edugrade.monsterhuntingboard.model.Appearance;

public record HunterResponse(
        Long id,
        String displayName,
        Appearance appearance,
        int level,
        int exp,
        int gold,
        int baseHp,
        int currentHp,
        int inventoryCapacity,
        List<InventoryItemResponse> inventory
) {
    public static HunterResponse from(Hunter hunter, List<HunterInventoryItem> inventoryItems, int inventoryCapacity) {
        return new HunterResponse(
                hunter.getId(),
                hunter.getDisplayName(),
                hunter.getAppearance(),
                hunter.getLevel(),
                hunter.getExp(),
                hunter.getGold(),
                hunter.getBaseHp(),
                hunter.getCurrentHp(),
                inventoryCapacity,
                inventoryItems.stream().map(InventoryItemResponse::from).toList()
        );
    }
}
