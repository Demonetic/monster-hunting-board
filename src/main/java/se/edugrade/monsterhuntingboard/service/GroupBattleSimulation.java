package se.edugrade.monsterhuntingboard.service;

import java.util.List;
import java.util.Map;
import se.edugrade.monsterhuntingboard.dto.BattleTurnResponse;

public record GroupBattleSimulation(
        boolean huntersWon,
        int bossRemainingHp,
        List<BattleTurnResponse> turns,
        Map<Long, HunterBattleOutcome> hunterOutcomes
) {
}
