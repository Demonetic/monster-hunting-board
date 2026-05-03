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
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.security.CustomUserDetailsService;
import se.edugrade.monsterhuntingboard.security.JwtAuthenticationFilter;
import se.edugrade.monsterhuntingboard.security.JwtService;
import se.edugrade.monsterhuntingboard.service.BeastService;

@WebMvcTest(BeastController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class BeastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BeastService beastService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllBeastsReturnsOk() throws Exception {
        when(beastService.getAllBeasts()).thenReturn(List.of(
                new BeastResponse(1L, BeastType.GRIFFIN, Difficulty.MEDIUM, 180, 35, 100, 75)
        ));

        mockMvc.perform(get("/api/beasts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("GRIFFIN"));
    }

    @Test
    void gameMasterCanCreateUpdateAndDeleteBeast() throws Exception {
        BeastResponse created = new BeastResponse(1L, BeastType.PHOENIX, Difficulty.HARD, 300, 55, 200, 150);
        BeastResponse updated = new BeastResponse(1L, BeastType.PHOENIX, Difficulty.HARD, 250, 45, 200, 150);

        when(beastService.createBeast(any())).thenReturn(created);
        when(beastService.updateBeast(eq(1L), any())).thenReturn(updated);
        doNothing().when(beastService).deleteBeast(1L);

        mockMvc.perform(post("/api/beasts")
                        .with(user("gm").roles("GAME_MASTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PHOENIX",
                                  "difficulty": "HARD",
                                  "hp": 300,
                                  "attackPower": 55,
                                  "rewardExp": 200,
                                  "rewardGold": 150
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PHOENIX"));

        mockMvc.perform(put("/api/beasts/1")
                        .with(user("gm").roles("GAME_MASTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hp": 250,
                                  "attackPower": 45
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hp").value(250))
                .andExpect(jsonPath("$.attackPower").value(45));

        mockMvc.perform(delete("/api/beasts/1")
                        .with(user("gm").roles("GAME_MASTER")))
                .andExpect(status().isNoContent());
    }

    @Test
    void hunterCannotModifyBeastsAndInvalidJsonFails() throws Exception {
        mockMvc.perform(post("/api/beasts")
                        .with(user("hunter").roles("HUNTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PHOENIX",
                                  "difficulty": "HARD",
                                  "hp": 300,
                                  "attackPower": 55,
                                  "rewardExp": 200,
                                  "rewardGold": 150
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/beasts/1")
                        .with(user("hunter").roles("HUNTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hp": 250
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/beasts/1")
                        .with(user("hunter").roles("HUNTER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/beasts")
                        .with(user("gm").roles("GAME_MASTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"GRIFFIN\",\"difficulty\":"))
                .andExpect(status().isBadRequest());
    }
}
