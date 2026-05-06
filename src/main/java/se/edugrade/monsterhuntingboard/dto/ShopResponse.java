package se.edugrade.monsterhuntingboard.dto;

import java.util.List;

public record ShopResponse(
        int hunterGold,
        int inventorySize,
        int inventoryCapacity,
        List<ShopItemResponse> items
) {
}
