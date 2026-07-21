package backend.xxx.chat.config;

import java.security.Principal;

import backend.xxx.chat.auth.service.CustomUserDetailsService;
import backend.xxx.chat.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    @Test
    void connectWithValidAccessTokenSetsPrincipal() {
        UserDetails userDetails = User.withUsername("alice")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        when(jwtService.extractUsername("access-token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(jwtService.isAccessTokenValid("access-token", userDetails)).thenReturn(true);

        Message<?> result = interceptor.preSend(connectMessage("Bearer access-token"), channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                result,
                StompHeaderAccessor.class
        );
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser())
                .isInstanceOf(UsernamePasswordAuthenticationToken.class)
                .extracting(Principal::getName)
                .isEqualTo("alice");
    }

    @Test
    void connectWithoutAccessTokenIsAllowedAsGuest() {
        Message<?> message = connectMessage(null);

        Message<?> result = interceptor.preSend(message, channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                result,
                StompHeaderAccessor.class
        );
        assertThat(result).isSameAs(message);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isNull();
    }

    @Test
    void subscribeToPublicMarketTopicIsAllowedWithoutAuthentication() {
        Message<?> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/market/tickers/BTCUSDT", null);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void subscribeToPrivateUserQueueRequiresAuthentication() {
        Message<?> message = stompMessage(StompCommand.SUBSCRIBE, "/user/queue/events", null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("stomp.frame.unauthorized");
    }

    @Test
    void sendToApplicationDestinationIsAllowedWhenAuthenticated() {
        UserDetails userDetails = User.withUsername("alice")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
        Message<?> message = stompMessage(StompCommand.SEND, "/app/conversations/1/typing", authentication);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    private Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authorization != null) {
            accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> stompMessage(
            StompCommand command,
            String destination,
            Principal principal
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}