package se.edugrade.monsterhuntingboard.dto;

public record InventoryActionResponse(
        HunterResponse hunter,
        String message
) {
}
