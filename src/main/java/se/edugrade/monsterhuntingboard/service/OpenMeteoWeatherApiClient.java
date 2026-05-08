package se.edugrade.monsterhuntingboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import se.edugrade.monsterhuntingboard.model.ResolvedLocation;

@Component
@RequiredArgsConstructor
public class OpenMeteoWeatherApiClient implements WeatherApiClient {
    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    private final RestClient.Builder restClientBuilder;

    @Override
    public List<ResolvedLocation> geocodeCity(String city, int count) {
        RestClient restClient = restClientBuilder.build();
        String uri = UriComponentsBuilder.fromHttpUrl(GEOCODING_URL)
                .queryParam("name", city)
                .queryParam("count", count)
                .queryParam("language", "en")
                .queryParam("format", "json")
                .toUriString();

        JsonNode root = restClient.get()
                .uri(uri)
                .retrieve()
                .body(JsonNode.class);

        JsonNode results = root == null ? null : root.path("results");
        if (results == null || results.isMissingNode() || !results.isArray()) {
            return List.of();
        }

        List<ResolvedLocation> locations = new ArrayList<>();
        for (JsonNode result : results) {
            locations.add(new ResolvedLocation(
                    result.path("name").asText(city),
                    result.path("country").asText(""),
                    result.path("latitude").asDouble(),
                    result.path("longitude").asDouble()
            ));
        }

        return locations;
    }

    @Override
    public ForecastSnapshot fetchCurrentWeather(double latitude, double longitude) {
        RestClient restClient = restClientBuilder.build();
        String uri = UriComponentsBuilder.fromHttpUrl(FORECAST_URL)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "weather_code,wind_speed_10m")
                .queryParam("timezone", "auto")
                .toUriString();

        JsonNode root = restClient.get()
                .uri(uri)
                .retrieve()
                .body(JsonNode.class);

        JsonNode current = root == null ? null : root.path("current");
        if (current == null || current.isMissingNode()) {
            throw new IllegalStateException("Weather API response missing current weather");
        }

        return new ForecastSnapshot(
                current.path("weather_code").asInt(),
                current.path("wind_speed_10m").asDouble()
        );
    }
}
