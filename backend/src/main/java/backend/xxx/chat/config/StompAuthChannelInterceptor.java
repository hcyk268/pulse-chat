package backend.xxx.chat.config;

import java.security.Principal;
import java.util.Set;

import backend.xxx.chat.auth.service.CustomUserDetailsService;
import backend.xxx.chat.auth.service.JwtService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String APPLICATION_DESTINATION_PREFIX = "/app/";
    private static final Set<String> ALLOWED_USER_SUBSCRIPTION_DESTINATIONS = Set.of(
            "/user/queue/events",
            "/user/queue/errors"
    );

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            return authenticateConnect(message, accessor);
        }

        if (StompCommand.DISCONNECT.equals(command) || StompCommand.UNSUBSCRIBE.equals(command)) {
            return message;
        }

        if (StompCommand.SEND.equals(command)) {
            requireAuthenticated(accessor);
            assertAllowedSendDestination(accessor.getDestination());
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            requireAuthenticated(accessor);
            assertAllowedSubscribeDestination(accessor.getDestination());
            return message;
        }

        throw new AccessDeniedException("STOMP command is not allowed");
    }

    private Message<?> authenticateConnect(Message<?> message, StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("Missing access token");
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        String username = jwtService.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isAccessTokenValid(token, userDetails)) {
            throw new AccessDeniedException("Invalid access token");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
        accessor.setUser(authentication);

        return message;
    }

    private void requireAuthenticated(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (!(user instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthorized STOMP frame");
        }
    }

    private void assertAllowedSendDestination(String destination) {
        if (destination == null || !destination.startsWith(APPLICATION_DESTINATION_PREFIX)) {
            throw new AccessDeniedException("SEND destination is not allowed");
        }
    }

    private void assertAllowedSubscribeDestination(String destination) {
        if (!ALLOWED_USER_SUBSCRIPTION_DESTINATIONS.contains(destination)) {
            throw new AccessDeniedException("SUBSCRIBE destination is not allowed");
        }
    }
}
