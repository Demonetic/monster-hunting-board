package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.Hunter;

public record BattleParticipantResponse(
        Long hunterId,
        String hunterName,
        String hunterAppearance,
        int initialHp,
        int initialMaxHp,
        int finalHp,
        boolean survived,
        int damageTaken,
        int damageDealt,
        int expChange,
        int goldChange
) {
    public static BattleParticipantResponse from(
            Hunter hunter,
            int initialHp,
            int initialMaxHp,
            int finalHp,
            boolean survived,
            int damageTaken,
            int damageDealt,
            int expChange,
            int goldChange
    ) {
        return new BattleParticipantResponse(
                hunter.getId(),
                hunter.getDisplayName(),
                hunter.getAppearance().name(),
                initialHp,
                initialMaxHp,
                finalHp,
                survived,
                damageTaken,
                damageDealt,
                expChange,
                goldChange
        );
    }
}
