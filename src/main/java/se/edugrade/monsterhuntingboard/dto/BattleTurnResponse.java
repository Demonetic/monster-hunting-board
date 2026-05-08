package se.edugrade.monsterhuntingboard.dto;

public record BattleTurnResponse(
        int turnNumber,
        String attacker,
        String attackerSide,
        String target,
        String targetSide,
        int damage,
        int targetHpAfter,
        int hunterHpAfter,
        int beastHpAfter,
        String message,
        String battleState,
        boolean criticalHit,
        boolean missed
) {
}
