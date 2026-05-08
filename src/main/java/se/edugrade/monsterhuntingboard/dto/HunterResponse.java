package se.edugrade.monsterhuntingboard.dto;

import java.util.List;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.HunterInventoryItem;
import se.edugrade.monsterhuntingboard.model.Appearance;

public record HunterResponse(
        Long id,
        String displayName,
        Appearance appearance,
        String appearanceDisplayName,
        String passiveSkillName,
        String passiveSkillDescription,
        String city,
        String country,
        double latitude,
        double longitude,
        int level,
        int exp,
        int gold,
        int baseHp,
        int currentHp,
        boolean expPotionActive,
        boolean endurancePotionActive,
        int inventoryCapacity,
        List<InventoryItemResponse> inventory
) {
    public static HunterResponse from(Hunter hunter, List<HunterInventoryItem> inventoryItems, int inventoryCapacity) {
        return new HunterResponse(
                hunter.getId(),
                hunter.getDisplayName(),
                hunter.getAppearance(),
                hunter.getAppearance().getDisplayName(),
                hunter.getAppearance().getPassiveSkillName(),
                hunter.getAppearance().getPassiveSkillDescription(),
                hunter.getCity(),
                hunter.getCountry(),
                hunter.getLatitude(),
                hunter.getLongitude(),
                hunter.getLevel(),
                hunter.getExp(),
                hunter.getGold(),
                hunter.getBaseHp(),
                hunter.getCurrentHp(),
                hunter.isExpPotionActive(),
                hunter.isEndurancePotionActive(),
                inventoryCapacity,
                inventoryItems.stream().map(InventoryItemResponse::from).toList()
        );
    }
}
