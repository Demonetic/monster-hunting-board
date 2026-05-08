package se.edugrade.monsterhuntingboard.service;

import java.util.List;
import se.edugrade.monsterhuntingboard.dto.BattleTurnResponse;

public record SoloBattleSimulation(
        int initialHunterHp,
        int initialMonsterHp,
        boolean hunterWon,
        int damageTaken,
        int hunterRemainingHp,
        List<BattleTurnResponse> turns
) {
}
