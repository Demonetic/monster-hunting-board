package se.edugrade.monsterhuntingboard.dto;

public record BattleTurnResponse(
        int turnNumber,
        String attacker,
        int damage,
        int hunterHpAfterTurn,
        int monsterHpAfterTurn
) {
}
