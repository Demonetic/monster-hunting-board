package se.edugrade.monsterhuntingboard.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import se.edugrade.monsterhuntingboard.security.CustomUserDetailsService;
import se.edugrade.monsterhuntingboard.security.JwtService;
import se.edugrade.monsterhuntingboard.service.ChatService;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final ChatService chatService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(
                            ServerHttpRequest request,
                            ServerHttpResponse response,
                            WebSocketHandler wsHandler,
                            java.util.Map<String, Object> attributes
                    ) {
                        log.info("WebSocket handshake attempt: uri={}, origin={}", request.getURI(), request.getHeaders().getOrigin());
                        return true;
                    }

                    @Override
                    public void afterHandshake(
                            ServerHttpRequest request,
                            ServerHttpResponse response,
                            WebSocketHandler wsHandler,
                            Exception exception
                    ) {
                        if (exception != null) {
                            log.warn("WebSocket handshake completed with exception: uri={}, message={}", request.getURI(), exception.getMessage());
                        } else {
                            log.info("WebSocket handshake completed: uri={}, status={}", request.getURI(), response.getHeaders());
                        }
                    }
                })
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://monster-hunter-board.duckdns.org",
                        "https://monster-hunter-board.duckdns.org"
                );
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    log.info("STOMP CONNECT received: sessionId={}", accessor.getSessionId());
                    authenticateWebSocketConnection(accessor);
                }

                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    log.info("STOMP SUBSCRIBE received: sessionId={}, destination={}", accessor.getSessionId(), accessor.getDestination());
                    authorizeSubscription(accessor);
                }

                return message;
            }
        });
    }

    private void authenticateWebSocketConnection(StompHeaderAccessor accessor) {
        String token = extractBearerToken(accessor.getNativeHeader("Authorization"));

        if (token == null) {
            log.warn("WebSocket CONNECT rejected: missing Authorization header");
            throw new IllegalArgumentException("Missing WebSocket authorization token");
        }

        String username = jwtService.extractUsername(token);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(token, userDetails)) {
            log.warn("WebSocket CONNECT rejected: invalid token for username={}", username);
            throw new IllegalArgumentException("Invalid WebSocket authorization token");
        }

        log.info("WebSocket CONNECT authenticated: username={}", username);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        ));
    }

    private String extractBearerToken(List<String> authorizationHeaders) {
        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            return null;
        }

        String authorizationHeader = authorizationHeaders.getFirst();
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        return authorizationHeader.substring(7);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith("/topic/chat/lobby/")) {
            return;
        }

        Authentication authentication = (Authentication) accessor.getUser();
        if (authentication == null) {
            log.warn("Lobby subscription rejected: missing authentication, destination={}", destination);
            throw new IllegalArgumentException("Missing WebSocket authentication");
        }

        Long lobbyId = Long.parseLong(destination.substring("/topic/chat/lobby/".length()));
        log.info("Authorizing lobby subscription: username={}, lobbyId={}", authentication.getName(), lobbyId);
        chatService.validateLobbySubscription(lobbyId, authentication.getName());
    }
}
