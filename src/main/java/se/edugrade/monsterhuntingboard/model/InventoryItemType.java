package se.edugrade.monsterhuntingboard.model;

public enum InventoryItemType {
    HEALTH_POTION("Health Potion", "Restores lost HP after battle", 30),
    EXP_POTION("EXP Potion", "Boosts earned EXP by 10%", 45),
    ENDURANCE_POTION("Endurance Potion", "Reduces monster damage taken", 55);

    private final String displayName;
    private final String description;
    private final int price;

    InventoryItemType(String displayName, String description, int price) {
        this.displayName = displayName;
        this.description = description;
        this.price = price;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }
}
