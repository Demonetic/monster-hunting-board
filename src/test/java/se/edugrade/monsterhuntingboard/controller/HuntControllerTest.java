package se.edugrade.monsterhuntingboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.edugrade.monsterhuntingboard.config.SecurityConfig;
import se.edugrade.monsterhuntingboard.dto.BeastResponse;
import se.edugrade.monsterhuntingboard.dto.HuntResponse;
import se.edugrade.monsterhuntingboard.dto.HuntResultResponse;
import se.edugrade.monsterhuntingboard.dto.JoinHuntResponse;
import se.edugrade.monsterhuntingboard.dto.ParticipantWeatherResponse;
import se.edugrade.monsterhuntingboard.dto.WeatherResponse;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntSourceType;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.WeatherCategory;
import se.edugrade.monsterhuntingboard.model.WeatherContext;
import se.edugrade.monsterhuntingboard.model.WeatherEffect;
import se.edugrade.monsterhuntingboard.security.CustomUserDetailsService;
import se.edugrade.monsterhuntingboard.security.JwtAuthenticationFilter;
import se.edugrade.monsterhuntingboard.security.JwtService;
import se.edugrade.monsterhuntingboard.service.HuntService;

@WebMvcTest(HuntController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HuntControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HuntService huntService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllHuntsReturnsOk() throws Exception {
        when(huntService.getAllHunts(null)).thenReturn(List.of(createHuntResponse(1L, "Scheduled Hunt", HuntStatus.SCHEDULED)));

        mockMvc.perform(get("/api/hunts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Scheduled Hunt"));
    }

    @Test
    void getScheduledHuntsReturnsOk() throws Exception {
        when(huntService.getScheduledHunts(null)).thenReturn(List.of(
                createHuntResponse(1L, "Scheduled Hunt", HuntStatus.SCHEDULED)
        ));

        mockMvc.perform(get("/api/hunts/scheduled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
    }

    @Test
    void gameMasterCanCreateUpdateAndDeleteHunt() throws Exception {
        HuntResponse created = createHuntResponse(1L, "Controller Hunt", HuntStatus.SCHEDULED);
        HuntResponse updated = createHuntResponse(1L, "Updated Hunt Title", HuntStatus.SCHEDULED);

        when(huntService.createHunt(any())).thenReturn(created);
        when(huntService.updateHunt(eq(1L), any())).thenReturn(updated);
        doNothing().when(huntService).deleteHunt(1L);

        mockMvc.perform(post("/api/hunts")
                        .with(user("gm").roles("GAME_MASTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Controller Hunt",
                                  "type": "HUNT",
                                  "difficulty": "EASY",
                                  "status": "SCHEDULED",
                                  "startTime": "2026-01-01T12:00:00",
                                  "maxPartySize": 3,
                                  "beastIds": [1],
                                  "rewardExp": 50,
                                  "rewardGold": 25
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Controller Hunt"));

        mockMvc.perform(put("/api/hunts/1")
                        .with(user("gm").roles("GAME_MASTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Hunt Title",
                                  "rewardExp": 150,
                                  "rewardGold": 90
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Hunt Title"));

        mockMvc.perform(delete("/api/hunts/1")
                        .with(user("gm").roles("GAME_MASTER")))
                .andExpect(status().isNoContent());
    }

    @Test
    void hunterCanJoinAndCompleteActiveHunt() throws Exception {
        when(huntService.joinHunt(1L, "aria")).thenReturn(
                new JoinHuntResponse(1L, "Active Hunt", 10L, "Aria", 1, 4, "Joined")
        );
        when(huntService.completeHuntForCurrentHunter(eq(1L), eq("aria"), any())).thenReturn(
                createHuntResultResponse(1L, "Active Hunt", true, 100, 75, 100, 75, 2, 110, 82, 18, true)
        );

        mockMvc.perform(post("/api/hunts/1/join")
                        .with(user("aria").roles("HUNTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.huntId").value(1))
                .andExpect(jsonPath("$.hunterDisplayName").value("Aria"));

        mockMvc.perform(post("/api/hunts/1/complete")
                        .with(user("aria").roles("HUNTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "won": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.won").value(true));
    }

    @Test
    void hunterCanStartSoloHunt() throws Exception {
        when(huntService.startSoloHunt(eq(1L), eq("solo"), any())).thenReturn(
                createHuntResultResponse(1L, "Solo Hunt", true, 50, 25, 50, 25, 1, 100, 88, 12, false)
        );

        mockMvc.perform(post("/api/hunts/1/solo/start")
                        .with(user("solo").roles("HUNTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "won": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.won").value(true));
    }

    @Test
    void permissionsAndInvalidJsonAreEnforcedForHuntEndpoints() throws Exception {
        mockMvc.perform(post("/api/hunts")
                        .with(user("hunter").roles("HUNTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Forbidden Hunt",
                                  "type": "HUNT",
                                  "difficulty": "EASY",
                                  "status": "SCHEDULED",
                                  "startTime": "2026-01-01T12:00:00",
                                  "maxPartySize": 3,
                                  "beastIds": [1],
                                  "rewardExp": 50,
                                  "rewardGold": 25
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/hunts/1")
                        .with(user("hunter").roles("HUNTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Blocked Update"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/hunts/1")
                        .with(user("hunter").roles("HUNTER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/hunts/1/join")
                        .with(user("gm").roles("GAME_MASTER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/hunts")
                        .with(user("gm").roles("GAME_MASTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Broken Hunt\",\"type\":\"HUNT\","))
                .andExpect(status().isBadRequest());
    }

    private HuntResponse createHuntResponse(Long id, String title, HuntStatus status) {
        return new HuntResponse(
                id,
                title,
                HuntType.HUNT,
                HuntSourceType.MANUAL,
                Difficulty.EASY,
                status,
                LocalDateTime.of(2026, 1, 1, 12, 0),
                3,
                List.of(new BeastResponse(1L, "Basilisk", BeastType.BASILISK, 100, 20, 50, 25)),
                0,
                50,
                25,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                0,
                null,
                false
        );
    }

    private HuntResultResponse createHuntResultResponse(
            Long huntId,
            String title,
            boolean won,
            int expChange,
            int goldChange,
            int newExp,
            int newGold,
            int newLevel,
            int newBaseHp,
            int newCurrentHp,
            int damageTaken,
            boolean endurancePotionApplied
    ) {
        return new HuntResultResponse(
                huntId,
                title,
                Difficulty.MEDIUM,
                10L,
                "Aria",
                "KNIGHT",
                100,
                100,
                "Griffin",
                "GRIFFIN",
                180,
                180,
                won,
                expChange,
                goldChange,
                newExp,
                newGold,
                newLevel,
                false,
                newBaseHp,
                newCurrentHp,
                damageTaken,
                false,
                endurancePotionApplied,
                WeatherResponse.from(new WeatherContext(
                        "Stockholm",
                        "Sweden",
                        59.3293,
                        18.0686,
                        0,
                        8.0,
                        17.0,
                        false,
                        WeatherCategory.SUNNY_CLEAR,
                        WeatherEffect.fromCategory(WeatherCategory.SUNNY_CLEAR)
                )),
                List.of(new ParticipantWeatherResponse(
                        10L,
                        "Aria",
                        WeatherResponse.from(new WeatherContext(
                                "Stockholm",
                                "Sweden",
                                59.3293,
                                18.0686,
                                0,
                                8.0,
                                17.0,
                                false,
                                WeatherCategory.SUNNY_CLEAR,
                                WeatherEffect.fromCategory(WeatherCategory.SUNNY_CLEAR)
                        ))
                )),
                List.of(),
                List.of()
        );
    }
}
