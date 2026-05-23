package backend.xxx.chat.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;

import backend.xxx.chat.auth.service.CustomUserDetailsService;
import backend.xxx.chat.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class StompAuthChannelInterceptorTest {

    private JwtService jwtService;
    private CustomUserDetailsService userDetailsService;
    private MessageChannel channel;
    private StompAuthChannelInterceptor interceptor;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        channel = mock(MessageChannel.class);
        interceptor = new StompAuthChannelInterceptor(jwtService, userDetailsService);

        UserDetails userDetails = User.withUsername("alice")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    @Test
    void connectWithValidAccessTokenSetsPrincipal() {
        UserDetails userDetails = User.withUsername("alice")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        when(jwtService.extractUsername("access-token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(jwtService.isAccessTokenValid("access-token", userDetails)).thenReturn(true);

        Message<?> result = interceptor.preSend(
                connectMessage("Bearer access-token"),
                channel
        );

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
    void connectWithoutAccessTokenIsDenied() {
        assertThatThrownBy(() -> interceptor.preSend(connectMessage(null), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Missing access token");
    }

    @Test
    void sendToApplicationDestinationIsAllowedWhenAuthenticated() {
        Message<?> message = stompMessage(
                StompCommand.SEND,
                "/app/conversations/1/typing",
                authentication
        );

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void sendToBrokerDestinationIsDenied() {
        Message<?> message = stompMessage(
                StompCommand.SEND,
                "/topic/conversations/1",
                authentication
        );

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("SEND destination is not allowed");
    }

    @Test
    void subscribeToUserEventQueueIsAllowedWhenAuthenticated() {
        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/user/queue/events",
                authentication
        );

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void subscribeToTopicIsDenied() {
        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/topic/conversations/1",
                authentication
        );

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("SUBSCRIBE destination is not allowed");
    }

    @Test
    void subscribeWithoutPrincipalIsDenied() {
        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/user/queue/events",
                null
        );

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Unauthorized STOMP frame");
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
