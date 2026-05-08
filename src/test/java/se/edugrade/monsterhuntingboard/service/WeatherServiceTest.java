package se.edugrade.monsterhuntingboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.ResolvedLocation;
import se.edugrade.monsterhuntingboard.model.WeatherCategory;

class WeatherServiceTest {

    private WeatherApiClient weatherApiClient;
    private se.edugrade.monsterhuntingboard.repository.HunterRepository hunterRepository;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherApiClient = mock(WeatherApiClient.class);
        hunterRepository = mock(se.edugrade.monsterhuntingboard.repository.HunterRepository.class);
        weatherService = new WeatherService(weatherApiClient, hunterRepository);
    }

    @Test
    void defaultLocationIsStockholm() {
        ResolvedLocation location = weatherService.resolveRegistrationLocation("");

        assertThat(location.city()).isEqualTo("Stockholm");
        assertThat(location.country()).isEqualTo("Sweden");
    }

    @Test
    void invalidCityThrowsUsefulError() {
        given(weatherApiClient.geocodeCity("Atlantis", 10)).willReturn(List.of());

        assertThatThrownBy(() -> weatherService.resolveCity("Atlantis"))
                .isInstanceOf(InvalidGameRuleException.class)
                .hasMessageContaining("No matching location found");
    }

    @Test
    void goteborgWithDiacriticsFallsBackToNormalizedSearch() {
        String city = "G\u00F6teborg";
        given(weatherApiClient.geocodeCity(city, 10)).willReturn(List.of());
        given(weatherApiClient.geocodeCity("Goteborg", 10)).willReturn(List.of(
                new ResolvedLocation("Goteborg", "Sweden", 57.7089, 11.9746)
        ));

        ResolvedLocation location = weatherService.resolveCity(city);

        assertThat(location.city()).isEqualTo(city);
        assertThat(location.country()).isEqualTo("Sweden");
        assertThat(location.latitude()).isEqualTo(57.7089);
    }

    @Test
    void malmoVaxjoOrebroAndStockholmResolveSuccessfully() {
        given(weatherApiClient.geocodeCity("Malm\u00F6", 10)).willReturn(List.of());
        given(weatherApiClient.geocodeCity("Malmo", 10)).willReturn(List.of(
                new ResolvedLocation("Malmo", "Sweden", 55.6050, 13.0038)
        ));
        given(weatherApiClient.geocodeCity("V\u00E4xj\u00F6", 10)).willReturn(List.of());
        given(weatherApiClient.geocodeCity("Vaxjo", 10)).willReturn(List.of(
                new ResolvedLocation("Vaxjo", "Sweden", 56.8790, 14.8059)
        ));
        given(weatherApiClient.geocodeCity("\u00D6rebro", 10)).willReturn(List.of());
        given(weatherApiClient.geocodeCity("Orebro", 10)).willReturn(List.of(
                new ResolvedLocation("Orebro", "Sweden", 59.2753, 15.2134)
        ));
        given(weatherApiClient.geocodeCity("Stockholm", 10)).willReturn(List.of(
                new ResolvedLocation("Stockholm", "Sweden", 59.3293, 18.0686)
        ));

        assertThat(weatherService.resolveCity("Malm\u00F6").city()).isEqualTo("Malm\u00F6");
        assertThat(weatherService.resolveCity("V\u00E4xj\u00F6").city()).isEqualTo("V\u00E4xj\u00F6");
        assertThat(weatherService.resolveCity("\u00D6rebro").city()).isEqualTo("\u00D6rebro");
        assertThat(weatherService.resolveCity("Stockholm").city()).isEqualTo("Stockholm");
    }

    @Test
    void goteborgStillResolvesDirectly() {
        given(weatherApiClient.geocodeCity("Goteborg", 10)).willReturn(List.of(
                new ResolvedLocation("Goteborg", "Sweden", 57.7089, 11.9746)
        ));

        assertThat(weatherService.resolveCity("Goteborg").city()).isEqualTo("Goteborg");
    }

    @Test
    void genericDiacriticNormalizationHandlesNordicAndEuropeanNames() {
        assertThat(weatherService.stripDiacritics("Malm\u00F6")).isEqualTo("Malmo");
        assertThat(weatherService.stripDiacritics("V\u00E4xj\u00F6")).isEqualTo("Vaxjo");
        assertThat(weatherService.stripDiacritics("J\u00F6nk\u00F6ping")).isEqualTo("Jonkoping");
        assertThat(weatherService.stripDiacritics("\u00D6rebro")).isEqualTo("Orebro");
        assertThat(weatherService.stripDiacritics("Z\u00FCrich")).isEqualTo("Zurich");
    }

    @Test
    void prefersDefaultCountryWhenMultipleResultsExist() {
        given(weatherApiClient.geocodeCity("Malm\u00F6", 10)).willReturn(List.of());
        given(weatherApiClient.geocodeCity("Malmo", 10)).willReturn(List.of(
                new ResolvedLocation("Malmo", "United States", 1.0, 1.0),
                new ResolvedLocation("Malm\u00F6", "Sweden", 55.6050, 13.0038)
        ));

        ResolvedLocation location = weatherService.resolveCity("Malm\u00F6");

        assertThat(location.country()).isEqualTo("Sweden");
        assertThat(location.city()).isEqualTo("Malm\u00F6");
    }

    @Test
    void weatherCodeMappingHandlesWindyAndExtremeCases() {
        assertThat(weatherService.mapWeatherCategory(0, 12.0)).isEqualTo(WeatherCategory.SUNNY_CLEAR);
        assertThat(weatherService.mapWeatherCategory(3, 32.0)).isEqualTo(WeatherCategory.WINDY);
        assertThat(weatherService.mapWeatherCategory(63, 12.0)).isEqualTo(WeatherCategory.RAIN);
        assertThat(weatherService.mapWeatherCategory(99, 20.0)).isEqualTo(WeatherCategory.EXTREME_WEATHER);
    }

    @Test
    void apiFailureFallsBackToCachedWeather() {
        Hunter hunter = defaultHunter();
        given(weatherApiClient.fetchCurrentWeather(59.3293, 18.0686))
                .willThrow(new IllegalStateException("api down"));

        insertStaleCachedWeather(hunter, WeatherCategory.SUNNY_CLEAR);

        assertThat(weatherService.getCurrentWeatherForHunter(hunter).fallback()).isTrue();
        assertThat(weatherService.getCurrentWeatherForHunter(hunter).category()).isEqualTo(WeatherCategory.SUNNY_CLEAR);
    }

    @Test
    void apiFailureWithoutCacheFallsBackToCloudy() {
        Hunter hunter = defaultHunter();
        given(weatherApiClient.fetchCurrentWeather(59.3293, 18.0686))
                .willThrow(new IllegalStateException("api down"));

        assertThat(weatherService.getCurrentWeatherForHunter(hunter).category()).isEqualTo(WeatherCategory.CLOUDY_OVERCAST);
        assertThat(weatherService.getCurrentWeatherForHunter(hunter).fallback()).isTrue();
    }

    @Test
    void weatherEffectsExposeExpectedSunnyRainWindyAndExtremeModifiers() {
        assertThat(se.edugrade.monsterhuntingboard.model.WeatherEffect.fromCategory(WeatherCategory.SUNNY_CLEAR).getGoldRewardMultiplier())
                .isEqualTo(1.10);
        assertThat(se.edugrade.monsterhuntingboard.model.WeatherEffect.fromCategory(WeatherCategory.RAIN).getBeastDamageMultiplier(se.edugrade.monsterhuntingboard.model.Difficulty.HARD))
                .isEqualTo(1.10);
        assertThat(se.edugrade.monsterhuntingboard.model.WeatherEffect.fromCategory(WeatherCategory.WINDY).getHunterAttackRollMultiplier(Appearance.RANGER))
                .isEqualTo(1.10);
        assertThat(se.edugrade.monsterhuntingboard.model.WeatherEffect.fromCategory(WeatherCategory.EXTREME_WEATHER).getExpRewardMultiplier())
                .isEqualTo(1.25);
    }

    private Hunter defaultHunter() {
        return Hunter.builder()
                .displayName("Aria")
                .appearance(Appearance.MAGE)
                .city("Stockholm")
                .country("Sweden")
                .latitude(59.3293)
                .longitude(18.0686)
                .level(1)
                .exp(0)
                .gold(0)
                .baseHp(100)
                .currentHp(100)
                .build();
    }

    @SuppressWarnings("unchecked")
    private void insertStaleCachedWeather(Hunter hunter, WeatherCategory category) {
        try {
            Field weatherCacheField = WeatherService.class.getDeclaredField("weatherCache");
            weatherCacheField.setAccessible(true);
            Map<String, Object> weatherCache = (Map<String, Object>) weatherCacheField.get(weatherService);

            Class<?> cachedWeatherClass = Class.forName(
                    "se.edugrade.monsterhuntingboard.service.WeatherService$CachedWeather"
            );
            Constructor<?> constructor = cachedWeatherClass.getDeclaredConstructor(
                    se.edugrade.monsterhuntingboard.model.WeatherContext.class,
                    Instant.class
            );
            constructor.setAccessible(true);

            Object cachedWeather = constructor.newInstance(
                    new se.edugrade.monsterhuntingboard.model.WeatherContext(
                            hunter.getCity(),
                            hunter.getCountry(),
                            hunter.getLatitude(),
                            hunter.getLongitude(),
                            0,
                            8.0,
                            false,
                            category,
                            se.edugrade.monsterhuntingboard.model.WeatherEffect.fromCategory(category)
                    ),
                    Instant.now().minus(2, ChronoUnit.HOURS)
            );

            String cacheKey = "%.4f:%.4f".formatted(hunter.getLatitude(), hunter.getLongitude());
            weatherCache.put(cacheKey, cachedWeather);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to seed weather cache for test", exception);
        }
    }
}
