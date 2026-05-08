package se.edugrade.monsterhuntingboard.model;

public enum Appearance {
    MAGE("Mage", "Mind of Study", "+10% EXP earned from hunt victories."),
    RANGER("Ranger", "Keen Volley", "Favours higher damage rolls in battle."),
    KNIGHT("Knight", "Iron Guard", "-2 damage taken from beast attacks."),
    PALADIN("Paladin", "Blessed Vitality", "+15 max HP."),
    HUNTER("Hunter", "Monster Slayer", "+4 maximum attack damage."),
    BARD("Bard", "Fortune Song", "20% chance to gain +10% extra gold on victories.");

    private final String displayName;
    private final String passiveSkillName;
    private final String passiveSkillDescription;

    Appearance(String displayName, String passiveSkillName, String passiveSkillDescription) {
        this.displayName = displayName;
        this.passiveSkillName = passiveSkillName;
        this.passiveSkillDescription = passiveSkillDescription;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPassiveSkillName() {
        return passiveSkillName;
    }

    public String getPassiveSkillDescription() {
        return passiveSkillDescription;
    }
}
