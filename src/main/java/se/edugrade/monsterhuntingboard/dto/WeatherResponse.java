package se.edugrade.monsterhuntingboard.dto;

import java.util.List;
import se.edugrade.monsterhuntingboard.model.WeatherContext;
import se.edugrade.monsterhuntingboard.model.WeatherEffect;

public record WeatherResponse(
        String city,
        String country,
        double latitude,
        double longitude,
        String category,
        String displayName,
        List<String> activeEffects,
        int weatherCode,
        double windSpeedKmh,
        double temperatureCelsius,
        boolean fallback,
        WeatherModifiersResponse modifiers
) {
    public static WeatherResponse from(WeatherContext context) {
        WeatherEffect effect = context.effect();
        return new WeatherResponse(
                context.city(),
                context.country(),
                context.latitude(),
                context.longitude(),
                context.category().name(),
                effect.displayName(),
                effect.descriptions(),
                context.weatherCode(),
                context.windSpeedKmh(),
                context.temperatureCelsius(),
                context.fallback(),
                WeatherModifiersResponse.from(effect)
        );
    }
}
