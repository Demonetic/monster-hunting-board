package se.edugrade.monsterhuntingboard.dto;

public record PurchaseItemResponse(
        HunterResponse hunter,
        InventoryItemResponse purchasedItem,
        int remainingGold,
        int inventorySize,
        int inventoryCapacity,
        String message
) {
}
