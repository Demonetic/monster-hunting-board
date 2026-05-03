package se.edugrade.monsterhuntingboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.edugrade.monsterhuntingboard.config.SecurityConfig;
import se.edugrade.monsterhuntingboard.dto.AuthResponse;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.security.CustomUserDetailsService;
import se.edugrade.monsterhuntingboard.security.JwtAuthenticationFilter;
import se.edugrade.monsterhuntingboard.security.JwtService;
import se.edugrade.monsterhuntingboard.service.AuthService;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void postRegisterReturnsCreated() throws Exception {
        AuthResponse response = new AuthResponse("token-123", "testhunter", Role.HUNTER);
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "testhunter",
                                  "password": "password123",
                                  "displayName": "Test Hunter",
                                  "appearance": "KNIGHT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-123"))
                .andExpect(jsonPath("$.role").value("HUNTER"));
    }

    @Test
    void postLoginReturnsOkAndToken() throws Exception {
        AuthResponse response = new AuthResponse("token-456", "loginhunter", Role.HUNTER);
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "loginhunter",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-456"))
                .andExpect(jsonPath("$.username").value("loginhunter"));
    }

    @Test
    void postRegisterWithInvalidBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": "123",
                                  "displayName": "",
                                  "appearance": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
