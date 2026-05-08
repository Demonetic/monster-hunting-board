package se.edugrade.monsterhuntingboard.model;

public record WeatherContext(
        String city,
        String country,
        double latitude,
        double longitude,
        int weatherCode,
        double windSpeedKmh,
        boolean fallback,
        WeatherCategory category,
        WeatherEffect effect
) {
}
