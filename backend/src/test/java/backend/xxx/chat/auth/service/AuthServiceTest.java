package backend.xxx.chat.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import backend.xxx.chat.auth.dto.ChangePasswordRequest;
import backend.xxx.chat.auth.dto.AuthResponse;
import backend.xxx.chat.auth.dto.RegisterResponse;
import backend.xxx.chat.auth.dto.ForgotPasswordRequest;
import backend.xxx.chat.auth.dto.LoginRequest;
import backend.xxx.chat.auth.dto.VerifyEmailRequest;
import backend.xxx.chat.auth.dto.RegisterRequest;
import backend.xxx.chat.auth.dto.ResetPasswordRequest;
import backend.xxx.chat.auth.exception.PasswordConfirmationMismatchException;
import backend.xxx.chat.auth.exception.UsernameAlreadyExistsException;
import backend.xxx.chat.auth.mail.AuthMailAsyncService;
import backend.xxx.chat.auth.model.RequestMetadata;
import backend.xxx.chat.common.security.CurrentUserProvider;

import backend.xxx.chat.common.util.TokenHash;
import backend.xxx.chat.config.properties.PasswordResetProperties;
import backend.xxx.chat.config.properties.EmailVerificationProperties;

import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
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
    private SetOperations<String, Object> setOperations;

    @Mock
    private TokenHash tokenHash;

    @Mock
    private AuthValidator authValidator;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private PasswordResetProperties passwordResetProperties;

    @Mock
    private EmailVerificationTokenService emailVerificationTokenService;

    @Mock
    private EmailVerificationProperties emailVerificationProperties;

    @Mock
    private AuthMailAsyncService authMailAsyncService;

    @Mock
    private RequestMetadataResolver requestMetadataResolver;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void registersUnverifiedUser() {
        RegisterRequest request = new RegisterRequest(
                " Alice ",
                " ALICE@EXAMPLE.COM ",
                "Alice",
                "Password123!",
                "Password123!"
        );
        Duration verificationTtl = Duration.ofHours(24);

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
        when(emailVerificationProperties.tokenTtl()).thenReturn(verificationTtl);
        when(emailVerificationProperties.frontendUrl()).thenReturn("https://app.example/verify-email");
        when(emailVerificationTokenService.createVerToken(10L, verificationTtl)).thenReturn("verify-token");
        RegisterResponse response = authService.register(request);

        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.verificationRequired()).isTrue();
        assertThat(response.verificationTokenExpiresInMs()).isEqualTo(verificationTtl.toMillis());
        verify(authValidator).validatePasswordConfirmation("Password123!", "Password123!");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("Alice");
        assertThat(savedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(savedUser.isEmailVerified()).isFalse();

        verify(authMailAsyncService).sendEmailVerification(
                "alice@example.com",
                "Alice",
                "https://app.example/verify-email?token=verify-token",
                "verify-token"
        );
    }

    @Test
    void rejectsDuplicateUsername() {
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
    void logsInUser() {
        LoginRequest request = new LoginRequest("alice", "Password123!");
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = User.create("alice", "alice@example.com", "hashed-password", "Alice");
        user.setId(10L);
        user.markEmailVerified(Instant.now());
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

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(requestMetadataResolver.current()).thenReturn(new RequestMetadata("127.0.0.1", "JUnit"));
        AuthResponse response = authService.login(request);

        assertThat(response).isSameAs(expectedResponse);
        verify(authenticationManager).authenticate(any());
        verify(valueOperations).set(eq("refresh:refresh-hash"), any(), eq(Duration.ofMillis(60_000L)));
verify(authMailAsyncService).sendLoginAlert(
                eq("alice@example.com"),
                eq("Alice"),
                eq("127.0.0.1"),
                eq("JUnit"),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private AuthResponse authResponse(String accessToken, String refreshToken) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", 30_000L, 60_000L, null);
    }

    @Test
    void sendsPasswordReset() {
        ForgotPasswordRequest request = new ForgotPasswordRequest(" ALICE@EXAMPLE.COM ");
        User user = User.create("alice", "alice@example.com", "old-hash", "Alice");
        user.setId(10L);
        user.markEmailVerified(Instant.now());

        when(authValidator.normalizeEmail(request.email())).thenReturn("alice@example.com");
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordResetProperties.tokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(passwordResetProperties.frontendUrl()).thenReturn("https://app.example/reset-password");
        when(passwordResetTokenService.issue(10L, Duration.ofMinutes(15))).thenReturn("reset-token");

        authService.forgotPassword(request);

        verify(authMailAsyncService).sendPasswordReset(
                "alice@example.com",
                "Alice",
                "https://app.example/reset-password?token=reset-token",
                "reset-token"
        );
    }

    @Test
    void hidesUnknownEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("missing@example.com");

        when(authValidator.normalizeEmail(request.email())).thenReturn("missing@example.com");
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(request);

        verify(passwordResetTokenService, never()).issue(any(), any());
        verify(authMailAsyncService, never()).sendPasswordReset(any(), any(), any(), any());
    }


    @Test
    void resetsPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "reset-token",
                "NewPassword123!",
                "NewPassword123!"
        );
        User user = User.create("alice", "alice@example.com", "old-hash", "Alice");
        user.setId(10L);

        when(passwordResetTokenService.consume("reset-token")).thenReturn(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hash");
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("refresh-user:10")).thenReturn(Set.of("hash-one", "hash-two"));

        authService.resetPassword(request);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getCredentialsVersion()).isEqualTo(1L);
        verify(authValidator).validatePasswordConfirmation("NewPassword123!", "NewPassword123!");
        verify(userRepository).save(user);
        verify(redisTemplate).delete(List.of("refresh:hash-one", "refresh:hash-two"));
        verify(redisTemplate).delete("refresh-user:10");
        verify(authMailAsyncService).sendPasswordChanged(eq("alice@example.com"), eq("Alice"), any(String.class));
    }

    @Test
    void rejectsResetMismatch() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "reset-token",
                "NewPassword123!",
                "DifferentPassword123!"
        );
        org.mockito.Mockito.doThrow(new PasswordConfirmationMismatchException())
                .when(authValidator)
                .validatePasswordConfirmation(request.newPassword(), request.confirmPassword());

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(PasswordConfirmationMismatchException.class);

        verify(passwordResetTokenService, never()).consume(any());
    }
    @Test
    void verifiesEmail() {
        VerifyEmailRequest request = new VerifyEmailRequest("verify-token");
        User user = User.create("alice", "alice@example.com", "old-hash", "Alice");
        user.setId(10L);

        when(emailVerificationTokenService.consume("verify-token")).thenReturn(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        authService.verifyEmail(request);

        assertThat(user.isEmailVerified()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void changesPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPassword123!",
                "NewPassword123!",
                "NewPassword123!"
        );
        User user = User.create("alice", "alice@example.com", "old-hash", "Alice");
        user.setId(10L);
        user.markEmailVerified(Instant.now());
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("alice")
                .password("new-hash")
                .authorities("ROLE_USER")
                .build();
        AuthResponse expectedResponse = authResponse("new-access-token", "new-refresh-token");

        when(currentUserProvider.getCurrentUsername()).thenReturn("alice");
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPassword123!", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword123!", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hash");
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("refresh-user:10")).thenReturn(Set.of());
        when(customUserDetailsService.toUserDetails(user)).thenReturn(userDetails);
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("new-refresh-token");
        when(jwtService.generateAccessToken(userDetails)).thenReturn("new-access-token");
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(60_000L);
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(30_000L);
        when(tokenHash.hashRefreshToken("new-refresh-token")).thenReturn("new-refresh-hash");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(authMapper.toResponse(user, "new-access-token", "new-refresh-token", 30_000L, 60_000L))
                .thenReturn(expectedResponse);

        AuthResponse response = authService.changePassword(request);

        assertThat(response).isSameAs(expectedResponse);
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getCredentialsVersion()).isEqualTo(1L);
        verify(redisTemplate).delete("refresh-user:10");
        verify(authMailAsyncService).sendPasswordChanged(eq("alice@example.com"), eq("Alice"), any(String.class));
    }
}
