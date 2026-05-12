package se.edugrade.monsterhuntingboard.model;

public record WeatherContext(
        String city,
        String country,
        double latitude,
        double longitude,
        int weatherCode,
        double windSpeedKmh,
        double temperatureCelsius,
        boolean fallback,
        WeatherCategory category,
        WeatherEffect effect
) {
}
