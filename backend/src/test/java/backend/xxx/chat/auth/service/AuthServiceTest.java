package backend.xxx.chat.auth.service;

import java.time.Duration;
import java.util.Optional;

import backend.xxx.chat.auth.dto.AuthResponse;
import backend.xxx.chat.auth.dto.LoginRequest;
import backend.xxx.chat.auth.dto.RegisterRequest;
import backend.xxx.chat.auth.exception.PasswordConfirmationMismatchException;
import backend.xxx.chat.auth.exception.UsernameAlreadyExistsException;
import backend.xxx.chat.common.util.TokenHash;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private TokenHash tokenHash;

    @Mock
    private AuthValidator authValidator;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesUserEncodesPasswordStoresRefreshSessionAndReturnsResponse() {
        RegisterRequest request = new RegisterRequest(
                " Alice ",
                " ALICE@EXAMPLE.COM ",
                "Alice",
                "Password123!",
                "Password123!"
        );
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("Alice")
                .password("hashed-password")
                .authorities("ROLE_USER")
                .build();
        AuthResponse expectedResponse = authResponse("access-token", "refresh-token");

        when(authValidator.normalizeUsername(" Alice ")).thenReturn("Alice");
        when(authValidator.normalizeEmail(" ALICE@EXAMPLE.COM ")).thenReturn("alice@example.com");
        when(userRepository.existsByUsernameIgnoreCase("Alice")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(customUserDetailsService.toUserDetails(any(User.class))).thenReturn(userDetails);
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        when(jwtService.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(60_000L);
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(30_000L);
        when(tokenHash.hashRefreshToken("refresh-token")).thenReturn("refresh-hash");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(authMapper.toResponse(any(User.class), eq("access-token"), eq("refresh-token"), eq(30_000L), eq(60_000L)))
                .thenReturn(expectedResponse);

        AuthResponse response = authService.register(request);

        assertThat(response).isSameAs(expectedResponse);
        verify(authValidator).validatePasswordConfirmation("Password123!", "Password123!");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("Alice");
        assertThat(savedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");

        verify(authValidator).validateRedisValue(eq("refresh:refresh-hash"), any(), eq(Duration.ofMillis(60_000L)));
        verify(valueOperations).set(eq("refresh:refresh-hash"), any(), eq(Duration.ofMillis(60_000L)));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegisterRequest request = new RegisterRequest(
                "alice",
                "alice@example.com",
                "Alice",
                "Password123!",
                "Password123!"
        );
        when(authValidator.normalizeUsername("alice")).thenReturn("alice");
        when(authValidator.normalizeEmail("alice@example.com")).thenReturn("alice@example.com");
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginAuthenticatesUserAndReturnsTokenResponse() {
        LoginRequest request = new LoginRequest("alice", "Password123!");
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = User.create("alice", "alice@example.com", "hashed-password", "Alice");
        user.setId(10L);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("alice")
                .password("hashed-password")
                .authorities("ROLE_USER")
                .build();
        AuthResponse expectedResponse = authResponse("access-token", "refresh-token");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getName()).thenReturn("alice");
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(user));
        when(customUserDetailsService.toUserDetails(user)).thenReturn(userDetails);
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        when(jwtService.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(60_000L);
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(30_000L);
        when(tokenHash.hashRefreshToken("refresh-token")).thenReturn("refresh-hash");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(authMapper.toResponse(user, "access-token", "refresh-token", 30_000L, 60_000L))
                .thenReturn(expectedResponse);

        AuthResponse response = authService.login(request);

        assertThat(response).isSameAs(expectedResponse);
        verify(authenticationManager).authenticate(any());
        verify(valueOperations).set(eq("refresh:refresh-hash"), any(), eq(Duration.ofMillis(60_000L)));
    }

    private AuthResponse authResponse(String accessToken, String refreshToken) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", 30_000L, 60_000L, null);
    }
}
