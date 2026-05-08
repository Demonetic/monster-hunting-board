package se.edugrade.monsterhuntingboard.service;

import se.edugrade.monsterhuntingboard.model.WeatherContext;
import se.edugrade.monsterhuntingboard.model.WeatherEffect;

public record GroupParticipantBattleContext(
        Long hunterId,
        String hunterName,
        WeatherContext weatherContext
) {
    public WeatherEffect weatherEffect() {
        return weatherContext.effect();
    }
}
