package se.edugrade.monsterhuntingboard.service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.ResolvedLocation;
import se.edugrade.monsterhuntingboard.model.WeatherCategory;
import se.edugrade.monsterhuntingboard.model.WeatherContext;
import se.edugrade.monsterhuntingboard.model.WeatherEffect;
import se.edugrade.monsterhuntingboard.repository.HunterRepository;

@Service
@RequiredArgsConstructor
public class WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final Duration WEATHER_CACHE_TTL = Duration.ofMinutes(45);
    private static final Duration LOCATION_CACHE_TTL = Duration.ofHours(12);
    private static final int GEOCODING_RESULT_COUNT = 10;
    private static final double WINDY_THRESHOLD_KMH = 30.0;
    private static final double EXTREME_WIND_THRESHOLD_KMH = 75.0;

    private final WeatherApiClient weatherApiClient;
    private final HunterRepository hunterRepository;

    private final Map<String, CachedLocation> locationCache = new ConcurrentHashMap<>();
    private final Map<String, CachedWeather> weatherCache = new ConcurrentHashMap<>();

    public ResolvedLocation getDefaultLocation() {
        return new ResolvedLocation(
                Hunter.DEFAULT_CITY,
                Hunter.DEFAULT_COUNTRY,
                Hunter.DEFAULT_LATITUDE,
                Hunter.DEFAULT_LONGITUDE
        );
    }

    public ResolvedLocation resolveRegistrationLocation(String city) {
        if (city == null || city.isBlank()) {
            return getDefaultLocation();
        }
        return resolveCity(city);
    }

    public ResolvedLocation resolveCity(String city) {
        String requestedCity = normalizeCity(city);
        String cacheKey = requestedCity.toLowerCase();
        CachedLocation cachedLocation = locationCache.get(cacheKey);
        if (cachedLocation != null && cachedLocation.isFresh()) {
            return cachedLocation.location();
        }

        Optional<ResolvedLocation> resolvedLocation = resolveCityCandidates(requestedCity);
        if (resolvedLocation.isEmpty()) {
            throw new InvalidGameRuleException("No matching location found for city: " + requestedCity);
        }

        ResolvedLocation candidate = resolvedLocation.get();
        ResolvedLocation location = new ResolvedLocation(
                requestedCity,
                candidate.country(),
                candidate.latitude(),
                candidate.longitude()
        );
        locationCache.put(cacheKey, new CachedLocation(location, Instant.now()));
        return location;
    }

    private Optional<ResolvedLocation> resolveCityCandidates(String requestedCity) {
        List<ResolvedLocation> exactCandidates = weatherApiClient.geocodeCity(requestedCity, GEOCODING_RESULT_COUNT);
        Optional<ResolvedLocation> exactMatch = pickBestLocation(exactCandidates, Hunter.DEFAULT_COUNTRY);
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        String normalizedSearch = stripDiacritics(requestedCity);
        if (normalizedSearch.equals(requestedCity)) {
            return Optional.empty();
        }

        List<ResolvedLocation> normalizedCandidates = weatherApiClient.geocodeCity(normalizedSearch, GEOCODING_RESULT_COUNT);
        return pickBestLocation(normalizedCandidates, Hunter.DEFAULT_COUNTRY);
    }

    private Optional<ResolvedLocation> pickBestLocation(List<ResolvedLocation> candidates, String preferredCountry) {
        return candidates.stream()
                .filter(candidate -> candidate.latitude() != 0d || candidate.longitude() != 0d)
                .min(Comparator
                        .comparing((ResolvedLocation candidate) -> !preferredCountry.equalsIgnoreCase(candidate.country()))
                        .thenComparing(candidate -> candidate.country().isBlank())
                        .thenComparing(ResolvedLocation::city, String.CASE_INSENSITIVE_ORDER));
    }

    public WeatherContext getCurrentWeatherForUsername(String username) {
        Hunter hunter = hunterRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Hunter not found for username: " + username));
        return getCurrentWeatherForHunter(hunter);
    }

    public WeatherContext getCurrentWeatherForHunter(Hunter hunter) {
        ResolvedLocation location = toLocation(hunter);
        String cacheKey = "%.4f:%.4f".formatted(location.latitude(), location.longitude());
        CachedWeather cachedWeather = weatherCache.get(cacheKey);
        if (cachedWeather != null && cachedWeather.isFresh()) {
            return cachedWeather.context();
        }

        try {
            WeatherApiClient.ForecastSnapshot forecastSnapshot =
                    weatherApiClient.fetchCurrentWeather(location.latitude(), location.longitude());
            WeatherCategory category = mapWeatherCategory(
                    forecastSnapshot.weatherCode(),
                    forecastSnapshot.windSpeedKmh()
            );
            WeatherContext context = new WeatherContext(
                    location.city(),
                    location.country(),
                    location.latitude(),
                    location.longitude(),
                    forecastSnapshot.weatherCode(),
                    forecastSnapshot.windSpeedKmh(),
                    false,
                    category,
                    WeatherEffect.fromCategory(category)
            );
            weatherCache.put(cacheKey, new CachedWeather(context, Instant.now()));
            return context;
        } catch (RuntimeException exception) {
            if (cachedWeather != null) {
                log.warn("Weather API failed for {}. Falling back to cached weather.", cacheKey, exception);
                WeatherContext staleContext = cachedWeather.context();
                return new WeatherContext(
                        staleContext.city(),
                        staleContext.country(),
                        staleContext.latitude(),
                        staleContext.longitude(),
                        staleContext.weatherCode(),
                        staleContext.windSpeedKmh(),
                        true,
                        staleContext.category(),
                        staleContext.effect()
                );
            }

            log.warn("Weather API failed for {}. Falling back to neutral weather.", cacheKey, exception);
            return new WeatherContext(
                    location.city(),
                    location.country(),
                    location.latitude(),
                    location.longitude(),
                    -1,
                    0.0,
                    true,
                    WeatherCategory.CLOUDY_OVERCAST,
                    WeatherEffect.fromCategory(WeatherCategory.CLOUDY_OVERCAST)
            );
        }
    }

    WeatherCategory mapWeatherCategory(int weatherCode, double windSpeedKmh) {
        if (weatherCode == 96 || weatherCode == 99 || windSpeedKmh >= EXTREME_WIND_THRESHOLD_KMH) {
            return WeatherCategory.EXTREME_WEATHER;
        }

        WeatherCategory baseCategory = switch (weatherCode) {
            case 0, 1 -> WeatherCategory.SUNNY_CLEAR;
            case 2, 3 -> WeatherCategory.CLOUDY_OVERCAST;
            case 45, 48 -> WeatherCategory.FOG_MIST;
            case 51, 53, 55, 56, 57 -> WeatherCategory.DRIZZLE;
            case 61, 63, 66, 80, 81 -> WeatherCategory.RAIN;
            case 65, 67, 82 -> WeatherCategory.HEAVY_RAIN;
            case 95 -> WeatherCategory.THUNDERSTORM;
            case 71, 73, 77, 85 -> WeatherCategory.SNOW;
            case 75, 86 -> WeatherCategory.HEAVY_SNOW;
            default -> WeatherCategory.CLOUDY_OVERCAST;
        };

        if (windSpeedKmh >= WINDY_THRESHOLD_KMH
                && (baseCategory == WeatherCategory.SUNNY_CLEAR
                || baseCategory == WeatherCategory.CLOUDY_OVERCAST)) {
            return WeatherCategory.WINDY;
        }

        return baseCategory;
    }

    private String normalizeCity(String city) {
        if (city == null || city.isBlank()) {
            throw new InvalidGameRuleException("City must not be empty");
        }
        return city.trim();
    }

    String stripDiacritics(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized
                .replace('\u00E5', 'a')
                .replace('\u00E4', 'a')
                .replace('\u00F6', 'o')
                .replace('\u00C5', 'A')
                .replace('\u00C4', 'A')
                .replace('\u00D6', 'O');
    }

    private ResolvedLocation toLocation(Hunter hunter) {
        String city = hunter.getCity() == null || hunter.getCity().isBlank()
                ? Hunter.DEFAULT_CITY
                : hunter.getCity();
        String country = hunter.getCountry() == null || hunter.getCountry().isBlank()
                ? Hunter.DEFAULT_COUNTRY
                : hunter.getCountry();
        double latitude = hunter.getLatitude() == 0d ? Hunter.DEFAULT_LATITUDE : hunter.getLatitude();
        double longitude = hunter.getLongitude() == 0d ? Hunter.DEFAULT_LONGITUDE : hunter.getLongitude();
        return new ResolvedLocation(city, country, latitude, longitude);
    }

    private record CachedLocation(
            ResolvedLocation location,
            Instant cachedAt
    ) {
        private boolean isFresh() {
            return cachedAt.plus(LOCATION_CACHE_TTL).isAfter(Instant.now());
        }
    }

    private record CachedWeather(
            WeatherContext context,
            Instant cachedAt
    ) {
        private boolean isFresh() {
            return cachedAt.plus(WEATHER_CACHE_TTL).isAfter(Instant.now());
        }
    }
}
