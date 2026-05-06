package se.edugrade.monsterhuntingboard.dto;

public record BattleTurnResponse(
        int turnNumber,
        String attacker,
        String target,
        int damage,
        String battleState
) {
}
