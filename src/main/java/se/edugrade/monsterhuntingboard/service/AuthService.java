package se.edugrade.monsterhuntingboard.service;

import jakarta.transaction.Transactional;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import se.edugrade.monsterhuntingboard.dto.AuthResponse;
import se.edugrade.monsterhuntingboard.dto.LoginRequest;
import se.edugrade.monsterhuntingboard.dto.RegisterRequest;
import se.edugrade.monsterhuntingboard.exception.DuplicateResourceException;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.ResolvedLocation;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;
import se.edugrade.monsterhuntingboard.repository.UserAccountRepository;
import se.edugrade.monsterhuntingboard.security.JwtService;
import se.edugrade.monsterhuntingboard.util.GameBalanceUtil;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final WeatherService weatherService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken");
        }

        UserAccount userAccount = UserAccount.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.HUNTER)
                .build();

        ResolvedLocation location = weatherService.resolveRegistrationLocation(request.city());
        int baseHp = GameBalanceUtil.calculateBaseHp(1, request.appearance());

        Hunter hunter = Hunter.builder()
                .displayName(request.displayName())
                .appearance(request.appearance())
                .city(location.city())
                .country(location.country())
                .latitude(location.latitude())
                .longitude(location.longitude())
                .level(1)
                .exp(0)
                .gold(0)
                .baseHp(baseHp)
                .currentHp(baseHp)
                .userAccount(userAccount)
                .build();

        userAccount.setHunter(hunter);
        hunter.setUserAccount(userAccount);

        UserAccount savedUserAccount = userAccountRepository.save(userAccount);
        String token = jwtService.generateToken(buildClaims(savedUserAccount), buildUserDetails(savedUserAccount));
        log.info("Registered user: {} (id={})", savedUserAccount.getUsername(), savedUserAccount.getId());

        return AuthResponse.from(token, savedUserAccount);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserAccount userAccount = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.username()));

        String token = jwtService.generateToken(buildClaims(userAccount), buildUserDetails(userAccount));
        log.info("User logged in: {}", userAccount.getUsername());
        return AuthResponse.from(token, userAccount);
    }

    private UserDetails buildUserDetails(UserAccount userAccount) {
        return User.builder()
                .username(userAccount.getUsername())
                .password(userAccount.getPassword())
                .authorities("ROLE_" + userAccount.getRole().name())
                .build();
    }

    private Map<String, Object> buildClaims(UserAccount userAccount) {
        return Map.of("role", userAccount.getRole().name());
    }
}
