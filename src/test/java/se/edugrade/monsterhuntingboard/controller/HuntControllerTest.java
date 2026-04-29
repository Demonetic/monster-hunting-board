package se.edugrade.monsterhuntingboard.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import se.edugrade.monsterhuntingboard.util.TestIds;
import se.edugrade.monsterhuntingboard.dto.AuthResponse;
import se.edugrade.monsterhuntingboard.dto.HuntResponse;
import se.edugrade.monsterhuntingboard.dto.LoginRequest;
import se.edugrade.monsterhuntingboard.dto.RegisterRequest;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;
import se.edugrade.monsterhuntingboard.repository.BeastRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;
import se.edugrade.monsterhuntingboard.repository.UserAccountRepository;
import se.edugrade.monsterhuntingboard.service.BattleService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HuntControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BeastRepository beastRepository;

    @Autowired
    private HuntRepository huntRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private BattleService battleService;

    @BeforeEach
    void setUp() {
        given(battleService.rollWin()).willReturn(true);
    }

    @Test
    void getAllHuntsReturnsOk() throws Exception {
        createScheduledHunt();

        mockMvc.perform(get("/api/hunts"))
                .andExpect(status().isOk());
    }

    @Test
    void getScheduledHuntsReturnsOk() throws Exception {
        createScheduledHunt();

        mockMvc.perform(get("/api/hunts/scheduled"))
                .andExpect(status().isOk());
    }

    @Test
    void gameMasterCanCreateUpdateAndDeleteHunt() throws Exception {
        String token = loginGameMasterAndGetToken();
        Beast beast = createBeast();

        MvcResult createResult = mockMvc.perform(post("/api/hunts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Controller Hunt",
                                  "type": "HUNT",
                                  "difficulty": "EASY",
                                  "status": "SCHEDULED",
                                  "startTime": "2026-01-01T12:00:00",
                                  "maxPartySize": 3,
                                  "beastIds": [%d],
                                  "rewardExp": 50,
                                  "rewardGold": 25
                                }
                                """.formatted(beast.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Controller Hunt"))
                .andReturn();

        HuntResponse createdHunt = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                HuntResponse.class
        );

        mockMvc.perform(put("/api/hunts/{id}", createdHunt.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Hunt Title",
                                  "rewardExp": 150,
                                  "rewardGold": 90
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Hunt Title"))
                .andExpect(jsonPath("$.rewardExp").value(150));

        mockMvc.perform(delete("/api/hunts/{id}", createdHunt.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void hunterCanJoinAndCompleteActiveHunt() throws Exception {
        String token = registerHunterAndGetToken("Complete Hunter");
        Hunt hunt = createActiveHunt();

        mockMvc.perform(post("/api/hunts/{id}/join", hunt.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.huntId").value(hunt.getId()));

        mockMvc.perform(post("/api/hunts/{id}/complete", hunt.getId())
                        .header("Authorization", "Bearer " + token)
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
        String token = registerHunterAndGetToken("Solo Hunter");
        Hunt hunt = createSoloHunt();

        mockMvc.perform(post("/api/hunts/{id}/solo/start", hunt.getId())
                        .header("Authorization", "Bearer " + token)
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
        String hunterToken = registerHunterAndGetToken("Restricted Hunter");
        String gmToken = loginGameMasterAndGetToken();
        Beast beast = createBeast();
        Hunt hunt = createActiveHunt();

        mockMvc.perform(post("/api/hunts")
                        .header("Authorization", "Bearer " + hunterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Forbidden Hunt",
                                  "type": "HUNT",
                                  "difficulty": "EASY",
                                  "status": "SCHEDULED",
                                  "startTime": "2026-01-01T12:00:00",
                                  "maxPartySize": 3,
                                  "beastIds": [%d],
                                  "rewardExp": 50,
                                  "rewardGold": 25
                                }
                                """.formatted(beast.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/hunts/{id}", hunt.getId())
                        .header("Authorization", "Bearer " + hunterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Blocked Update"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/hunts/{id}", hunt.getId())
                        .header("Authorization", "Bearer " + hunterToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/hunts/{id}/join", hunt.getId())
                        .header("Authorization", "Bearer " + gmToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/hunts")
                        .header("Authorization", "Bearer " + gmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Broken Hunt\",\"type\":\"HUNT\","))
                .andExpect(status().isBadRequest());
    }

    private Beast createBeast() {
        return beastRepository.save(Beast.builder()
                .type(BeastType.BASILISK)
                .difficulty(Difficulty.EASY)
                .hp(100)
                .attackPower(20)
                .rewardExp(50)
                .rewardGold(25)
                .build());
    }

    private Hunt createScheduledHunt() {
        Beast beast = createBeast();
        return huntRepository.save(Hunt.builder()
                .title("Scheduled Hunt " + TestIds.shortId())
                .type(HuntType.HUNT)
                .difficulty(Difficulty.MEDIUM)
                .status(HuntStatus.SCHEDULED)
                .startTime(LocalDateTime.now().plusHours(2))
                .maxPartySize(4)
                .beasts(List.of(beast))
                .rewardExp(100)
                .rewardGold(75)
                .build());
    }

    private Hunt createActiveHunt() {
        Beast beast = createBeast();
        return huntRepository.save(Hunt.builder()
                .title("Active Hunt " + TestIds.shortId())
                .type(HuntType.HUNT)
                .difficulty(Difficulty.MEDIUM)
                .status(HuntStatus.ACTIVE)
                .startTime(LocalDateTime.now().plusHours(2))
                .maxPartySize(4)
                .beasts(List.of(beast))
                .rewardExp(100)
                .rewardGold(75)
                .build());
    }

    private Hunt createSoloHunt() {
        Beast beast = createBeast();
        return huntRepository.save(Hunt.builder()
                .title("Solo Hunt " + TestIds.shortId())
                .type(HuntType.SOLO_HUNT)
                .difficulty(Difficulty.EASY)
                .status(HuntStatus.ACTIVE)
                .beasts(List.of(beast))
                .rewardExp(50)
                .rewardGold(25)
                .build());
    }

    private String registerHunterAndGetToken(String displayName) throws Exception {
        String username = "h-" + TestIds.shortId();
        RegisterRequest request = new RegisterRequest(username, "password123", displayName, Appearance.KNIGHT);

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
