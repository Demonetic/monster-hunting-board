package se.edugrade.monsterhuntingboard.service;

import java.util.List;
import se.edugrade.monsterhuntingboard.model.ResolvedLocation;

public interface WeatherApiClient {

    List<ResolvedLocation> geocodeCity(String city, int count);

    ForecastSnapshot fetchCurrentWeather(double latitude, double longitude);

    record ForecastSnapshot(
            int weatherCode,
            double windSpeedKmh
    ) {
    }
}
