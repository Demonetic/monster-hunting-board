package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.InventoryItemType;

public record ShopItemResponse(
        InventoryItemType itemType,
        String displayName,
        String description,
        int price
) {
    public static ShopItemResponse from(InventoryItemType itemType) {
        return new ShopItemResponse(
                itemType,
                itemType.getDisplayName(),
                itemType.getDescription(),
                itemType.getPrice()
        );
    }
}
