package se.edugrade.monsterhuntingboard.config;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import se.edugrade.monsterhuntingboard.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final RequestMatcher SPA_GET_REQUESTS =
            RegexRequestMatcher.regexMatcher(HttpMethod.GET, "^/(?!(api|ws)(?:/|$)).*$");
    private static final String AUTH_PATH = "/api/auth/**";
    private static final String WEB_SOCKET_PATH = "/ws";
    private static final String WEB_SOCKET_PATHS = "/ws/**";
    private static final String ACTUATOR_HEALTH_PATH = "/actuator/health";
    private static final String SWAGGER_UI_ROOT = "/swagger-ui.html";
    private static final String SWAGGER_UI_PATH = "/swagger-ui/**";
    private static final String API_DOCS_ROOT = "/v3/api-docs";
    private static final String API_DOCS_PATH = "/v3/api-docs/**";
    private static final String BEASTS_PATH = "/api/beasts/**";
    private static final String HUNTS_PATH = "/api/hunts/**";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WEB_SOCKET_PATH, WEB_SOCKET_PATHS).permitAll()
                        .requestMatchers(SPA_GET_REQUESTS).permitAll()
                        .requestMatchers(AUTH_PATH, ACTUATOR_HEALTH_PATH, SWAGGER_UI_ROOT, SWAGGER_UI_PATH, API_DOCS_PATH, API_DOCS_ROOT)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, BEASTS_PATH, HUNTS_PATH).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "https://monster-hunter-board.duckdns.org"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
