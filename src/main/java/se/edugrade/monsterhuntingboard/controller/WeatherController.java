package se.edugrade.monsterhuntingboard.controller;

import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.edugrade.monsterhuntingboard.dto.WeatherResponse;
import se.edugrade.monsterhuntingboard.service.WeatherService;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<WeatherResponse> getCurrentWeather(Principal principal) {
        return ResponseEntity.ok(WeatherResponse.from(weatherService.getCurrentWeatherForUsername(principal.getName())));
    }
}
