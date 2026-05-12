package se.edugrade.monsterhuntingboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.edugrade.monsterhuntingboard.config.SecurityConfig;
import se.edugrade.monsterhuntingboard.model.WeatherCategory;
import se.edugrade.monsterhuntingboard.model.WeatherContext;
import se.edugrade.monsterhuntingboard.model.WeatherEffect;
import se.edugrade.monsterhuntingboard.security.CustomUserDetailsService;
import se.edugrade.monsterhuntingboard.security.JwtAuthenticationFilter;
import se.edugrade.monsterhuntingboard.security.JwtService;
import se.edugrade.monsterhuntingboard.service.WeatherService;

@WebMvcTest(WeatherController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherService weatherService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getCurrentWeatherReturnsWeatherPayload() throws Exception {
        when(weatherService.getCurrentWeatherForUsername("aria")).thenReturn(new WeatherContext(
                "Stockholm",
                "Sweden",
                59.3293,
                18.0686,
                0,
                12.0,
                21.0,
                false,
                WeatherCategory.SUNNY_CLEAR,
                WeatherEffect.fromCategory(WeatherCategory.SUNNY_CLEAR)
        ));

        mockMvc.perform(get("/api/weather/current")
                        .with(user("aria").roles("HUNTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("SUNNY_CLEAR"))
                .andExpect(jsonPath("$.city").value("Stockholm"))
                .andExpect(jsonPath("$.activeEffects[0]").value("+10% gold earned"));
    }
}
