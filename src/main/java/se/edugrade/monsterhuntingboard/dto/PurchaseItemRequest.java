package se.edugrade.monsterhuntingboard.dto;

import jakarta.validation.constraints.NotNull;
import se.edugrade.monsterhuntingboard.model.InventoryItemType;

public record PurchaseItemRequest(
        @NotNull InventoryItemType itemType
) {
}
