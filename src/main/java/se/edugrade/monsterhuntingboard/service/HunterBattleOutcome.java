package se.edugrade.monsterhuntingboard.service;

public record HunterBattleOutcome(
        int remainingHp,
        int damageTaken,
        int damageDealt
) {
}
