package se.edugrade.monsterhuntingboard.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import se.edugrade.monsterhuntingboard.util.TestIds;
import se.edugrade.monsterhuntingboard.dto.AuthResponse;
import se.edugrade.monsterhuntingboard.dto.BattleTurnResponse;
import se.edugrade.monsterhuntingboard.dto.BeastRequest;
import se.edugrade.monsterhuntingboard.dto.BeastResponse;
import se.edugrade.monsterhuntingboard.dto.CompleteHuntRequest;
import se.edugrade.monsterhuntingboard.dto.CreateHuntRequest;
import se.edugrade.monsterhuntingboard.dto.HuntResponse;
import se.edugrade.monsterhuntingboard.dto.HuntResultResponse;
import se.edugrade.monsterhuntingboard.dto.JoinHuntResponse;
import se.edugrade.monsterhuntingboard.dto.LoginRequest;
import se.edugrade.monsterhuntingboard.dto.RegisterRequest;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.HuntParticipation;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.ResolvedLocation;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;
import se.edugrade.monsterhuntingboard.model.WeatherCategory;
import se.edugrade.monsterhuntingboard.model.WeatherContext;
import se.edugrade.monsterhuntingboard.model.WeatherEffect;
import se.edugrade.monsterhuntingboard.repository.UserAccountRepository;
import se.edugrade.monsterhuntingboard.service.BattleService;
import se.edugrade.monsterhuntingboard.service.GroupBattleSimulation;
import se.edugrade.monsterhuntingboard.service.GroupParticipantBattleContext;
import se.edugrade.monsterhuntingboard.service.HunterBattleOutcome;
import se.edugrade.monsterhuntingboard.service.WeatherService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MonsterHuntingBoardIntegrationTest {
    private static final ZoneId STOCKHOLM_ZONE = ZoneId.of("Europe/Stockholm");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private BattleService battleService;

    @MockitoBean
    private WeatherService weatherService;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        given(weatherService.resolveRegistrationLocation(any())).willReturn(
                new ResolvedLocation("Stockholm", "Sweden", 59.3293, 18.0686)
        );
        given(weatherService.getCurrentWeatherForHunter(any())).willReturn(new WeatherContext(
                "Stockholm",
                "Sweden",
                59.3293,
                18.0686,
                0,
                8.0,
                false,
                WeatherCategory.SUNNY_CLEAR,
                WeatherEffect.fromCategory(WeatherCategory.SUNNY_CLEAR)
        ));

        given(battleService.simulateGroupBossBattle(any(), any(), any())).willAnswer(invocation -> {
            List<HuntParticipation> participations = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            Map<Long, GroupParticipantBattleContext> participantWeatherContexts = invocation.getArgument(2);
            Long hunterId = participations.getFirst().getHunter().getId();

            return new GroupBattleSimulation(
                    15,
                    true,
                    0,
                    List.of(new BattleTurnResponse(
                            1,
                            "Integration Hunter",
                            "hunter",
                            "GRIFFIN",
                            "beast",
                            15,
                            0,
                            88,
                            0,
                            "Griffin takes 15 damage",
                            "Integration Hunter: 88 HP | Boss HP: 0",
                            false,
                            false
                    )),
                    Map.of(hunterId, new HunterBattleOutcome(88, 12)),
                    participantWeatherContexts
            );
        });
    }

    @Test
    void completeFlowWorks() {
        String hunterUsername = "h-" + TestIds.shortId();
        RegisterRequest registerRequest = new RegisterRequest(
                hunterUsername,
                "password123",
                "Integration Hunter",
                null,
                Appearance.KNIGHT
        );

        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                registerRequest,
                AuthResponse.class
        );
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String hunterToken = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                new LoginRequest(hunterUsername, "password123"),
                AuthResponse.class
        ).getBody().token();

        String gmUsername = "gm-" + TestIds.shortId();
        userAccountRepository.save(UserAccount.builder()
                .username(gmUsername)
                .password(passwordEncoder.encode("password123"))
                .role(Role.GAME_MASTER)
                .build());
        String gmToken = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                new LoginRequest(gmUsername, "password123"),
                AuthResponse.class
        ).getBody().token();

        ResponseEntity<BeastResponse> beastResponse = restTemplate.exchange(
                baseUrl + "/api/beasts",
                HttpMethod.POST,
                authorizedEntity(
                        gmToken,
                        new BeastRequest(
                                "Griffin",
                                se.edugrade.monsterhuntingboard.model.BeastType.GRIFFIN,
                                200,
                                40,
                                120,
                                80
                        )
                ),
                BeastResponse.class
        );
        assertThat(beastResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<HuntResponse> huntResponse = restTemplate.exchange(
                baseUrl + "/api/hunts",
                HttpMethod.POST,
                authorizedEntity(
                        gmToken,
                        new CreateHuntRequest(
                                "Integration Hunt",
                                HuntType.HUNT,
                                Difficulty.BOSS,
                                HuntStatus.ACTIVE,
                                futureStockholmTime(4),
                                3,
                                List.of(beastResponse.getBody().id()),
                                50,
                                25
                        )
                ),
                HuntResponse.class
        );
        assertThat(huntResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<JoinHuntResponse> joinResponse = restTemplate.exchange(
                baseUrl + "/api/hunts/" + huntResponse.getBody().id() + "/join",
                HttpMethod.POST,
                authorizedEntity(hunterToken),
                JoinHuntResponse.class
        );
        assertThat(joinResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<HuntResultResponse> completeResponse = restTemplate.exchange(
                baseUrl + "/api/hunts/" + huntResponse.getBody().id() + "/complete",
                HttpMethod.POST,
                authorizedEntity(hunterToken, new CompleteHuntRequest(true)),
                HuntResultResponse.class
        );

        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completeResponse.getBody()).isNotNull();
        assertThat(completeResponse.getBody().won()).isTrue();
        assertThat(completeResponse.getBody().expChange()).isGreaterThan(0);
        assertThat(completeResponse.getBody().goldChange()).isGreaterThan(0);
    }

    @Test
    void roleRestrictionsWorkAcrossEndpoints() {
        String hunterUsername = "h-" + TestIds.shortId();
        restTemplate.postForEntity(
                baseUrl + "/api/auth/register",
                new RegisterRequest(hunterUsername, "password123", "Restricted Hunter", null, Appearance.RANGER),
                AuthResponse.class
        );
        String hunterToken = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                new LoginRequest(hunterUsername, "password123"),
                AuthResponse.class
        ).getBody().token();

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/hunts",
                HttpMethod.POST,
                authorizedEntity(
                        hunterToken,
                        new CreateHuntRequest(
                                "Forbidden Hunt",
                                HuntType.HUNT,
                                Difficulty.BOSS,
                                HuntStatus.SCHEDULED,
                                java.time.LocalDateTime.of(2026, 1, 1, 12, 0),
                                2,
                                List.of(999L),
                                50,
                                25
                        )
                ),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpEntity<Void> authorizedEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> authorizedEntity(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private LocalDateTime futureStockholmTime(int hoursAhead) {
        return ZonedDateTime.now(STOCKHOLM_ZONE).toLocalDateTime().plusHours(hoursAhead);
    }
}
