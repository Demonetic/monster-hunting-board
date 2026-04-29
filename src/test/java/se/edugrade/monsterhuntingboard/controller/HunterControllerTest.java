package se.edugrade.monsterhuntingboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import se.edugrade.monsterhuntingboard.util.TestIds;
import se.edugrade.monsterhuntingboard.dto.AuthResponse;
import se.edugrade.monsterhuntingboard.dto.LoginRequest;
import se.edugrade.monsterhuntingboard.dto.RegisterRequest;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;
import se.edugrade.monsterhuntingboard.repository.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HunterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getCurrentHunterWithTokenReturnsOk() throws Exception {
        String token = registerHunterAndGetToken("Aria");

        mockMvc.perform(get("/api/hunters/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Aria"));
    }

    @Test
    void patchAppearanceReturnsOkAndBardIsRejected() throws Exception {
        String token = registerHunterAndGetToken("Appearance Hunter");

        mockMvc.perform(patch("/api/hunters/me/appearance")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appearance": "PALADIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appearance").value("PALADIN"));

        mockMvc.perform(patch("/api/hunters/me/appearance")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appearance": "BARD"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hunterAndGameMasterSeeCorrectHunterEndpointPermissions() throws Exception {
        String hunterToken = registerHunterAndGetToken("Listed Hunter");
        String gmToken = loginGameMasterAndGetToken();

        mockMvc.perform(get("/api/hunters/me")
                        .header("Authorization", "Bearer " + gmToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/hunters/admin/all")
                        .header("Authorization", "Bearer " + gmToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/hunters/admin/all")
                        .header("Authorization", "Bearer " + hunterToken))
                .andExpect(status().isForbidden());
    }

    private String registerHunterAndGetToken(String displayName) throws Exception {
        String username = "h-" + TestIds.shortId();
        RegisterRequest request = new RegisterRequest(username, "password123", displayName, Appearance.KNIGHT);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return response.token();
    }

    private String loginGameMasterAndGetToken() throws Exception {
        String username = "gm-" + TestIds.shortId();
        userAccountRepository.save(UserAccount.builder()
                .username(username)
                .password(passwordEncoder.encode("password123"))
                .role(Role.GAME_MASTER)
                .build());

        LoginRequest request = new LoginRequest(username, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return response.token();
    }
}
