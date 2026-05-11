package se.edugrade.monsterhuntingboard.dto;

public record BattleTurnResponse(
        int turnNumber,
        String attackerCombatantId,
        String attacker,
        String attackerSide,
        String targetCombatantId,
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
