package se.edugrade.monsterhuntingboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import se.edugrade.monsterhuntingboard.dto.BeastResponse;
import se.edugrade.monsterhuntingboard.dto.LoginRequest;
import se.edugrade.monsterhuntingboard.dto.RegisterRequest;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;
import se.edugrade.monsterhuntingboard.repository.BeastRepository;
import se.edugrade.monsterhuntingboard.repository.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BeastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BeastRepository beastRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void gameMasterCanCreateUpdateAndDeleteBeast() throws Exception {
        String token = loginGameMasterAndGetToken();

        MvcResult createResult = mockMvc.perform(post("/api/beasts")
                        .header("Authorization", "Bearer " + token)
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
                .andExpect(jsonPath("$.type").value("PHOENIX"))
                .andReturn();

        BeastResponse createdBeast = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                BeastResponse.class
        );

        mockMvc.perform(put("/api/beasts/{id}", createdBeast.id())
                        .header("Authorization", "Bearer " + token)
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

        mockMvc.perform(delete("/api/beasts/{id}", createdBeast.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void hunterCannotModifyBeastsAndInvalidJsonFails() throws Exception {
        String hunterToken = registerHunterAndGetToken();
        String gmToken = loginGameMasterAndGetToken();
        Beast beast = beastRepository.save(Beast.builder()
                .type(BeastType.BASILISK)
                .difficulty(Difficulty.EASY)
                .hp(100)
                .attackPower(20)
                .rewardExp(50)
                .rewardGold(25)
                .build());

        mockMvc.perform(post("/api/beasts")
                        .header("Authorization", "Bearer " + hunterToken)
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

        mockMvc.perform(put("/api/beasts/{id}", beast.getId())
                        .header("Authorization", "Bearer " + hunterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hp": 250
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/beasts/{id}", beast.getId())
                        .header("Authorization", "Bearer " + hunterToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/beasts")
                        .header("Authorization", "Bearer " + gmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"GRIFFIN\",\"difficulty\":"))
                .andExpect(status().isBadRequest());
    }

    private String registerHunterAndGetToken() throws Exception {
        String username = "h-" + TestIds.shortId();
        RegisterRequest request = new RegisterRequest(username, "password123", "Beast Hunter", Appearance.KNIGHT);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class).token();
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

        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class).token();
    }
}
