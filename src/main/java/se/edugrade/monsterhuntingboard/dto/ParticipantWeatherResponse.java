package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.service.GroupParticipantBattleContext;

public record ParticipantWeatherResponse(
        Long hunterId,
        String hunterName,
        WeatherResponse weather
) {
    public static ParticipantWeatherResponse from(GroupParticipantBattleContext context) {
        return new ParticipantWeatherResponse(
                context.hunterId(),
                context.hunterName(),
                WeatherResponse.from(context.weatherContext())
        );
    }
}
