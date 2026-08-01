package backend.xxx.chat.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import backend.xxx.chat.auth.dto.AuthResponse;
import backend.xxx.chat.auth.dto.ChangePasswordRequest;
import backend.xxx.chat.auth.dto.RegisterResponse;
import backend.xxx.chat.auth.dto.ForgotPasswordRequest;
import backend.xxx.chat.auth.dto.LoginRequest;
import backend.xxx.chat.auth.dto.RefreshTokenRequest;
import backend.xxx.chat.auth.dto.RegisterRequest;
import backend.xxx.chat.auth.dto.ResetPasswordRequest;
import backend.xxx.chat.auth.exception.EmailAlreadyExistsException;
import backend.xxx.chat.auth.dto.ResendVerificationRequest;
import backend.xxx.chat.auth.dto.VerifyEmailRequest;
import backend.xxx.chat.auth.exception.InvalidPasswordResetTokenException;
import backend.xxx.chat.auth.exception.InvalidRefreshTokenException;
import backend.xxx.chat.auth.exception.EmailVerificationRequiredException;
import backend.xxx.chat.auth.exception.InvalidCurrentPasswordException;
import backend.xxx.chat.auth.exception.InvalidEmailVerificationTokenException;
import backend.xxx.chat.auth.exception.PasswordReuseException;
import backend.xxx.chat.auth.exception.RedisUnavailable;
import backend.xxx.chat.auth.exception.UsernameAlreadyExistsException;
import backend.xxx.chat.auth.model.RefreshTokenSession;
import backend.xxx.chat.common.exception.AccountInactiveException;
import backend.xxx.chat.common.exception.AccountLockedException;
import backend.xxx.chat.auth.mail.AuthMailAsyncService;
import backend.xxx.chat.auth.model.RequestMetadata;
import backend.xxx.chat.common.exception.UnauthorizedException;
import backend.xxx.chat.common.util.TokenHash;
import backend.xxx.chat.config.properties.PasswordResetProperties;

import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.config.properties.EmailVerificationProperties;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";

    private static final String USER_REFRESH_TOKENS_KEY_PREFIX = "refresh-user:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenHash tokenHash;
    private final AuthValidator authValidator;

    private final PasswordResetTokenService passwordResetTokenService;
    private final PasswordResetProperties passwordResetProperties;

    private final EmailVerificationTokenService emailVerificationTokenService;
    private final EmailVerificationProperties emailVerificationProperties;
    private final AuthMailAsyncService authMailAsyncService;
    private final RequestMetadataResolver requestMetadataResolver;
    private final CurrentUserProvider currentUserProvider;


    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        authValidator.validatePasswordConfirmation(request.password(), request.confirmPassword());

        String normalizedUsername = authValidator.normalizeUsername(request.username());
        String normalizedEmail = authValidator.normalizeEmail(request.email());

        checkUserRegisterUnique(normalizedUsername, normalizedEmail);

        User user = User.create(
                normalizedUsername,
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.displayName()
        );
        user = userRepository.save(user);

        sendVerificationEmail(user);

        return new RegisterResponse(
                user.getEmail(),
                true,
                emailVerificationProperties.tokenTtl().toMillis()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password())
        );

        String username = authentication.getName();
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UnauthorizedException("auth.invalid.credentials"));

        assertAccountStatus(user);
        AuthResponse response = toAuthResponse(user);
        RequestMetadata metadata = requestMetadataResolver.current();
        authMailAsyncService.sendLoginAlert(
                user.getEmail(),
                user.getDisplayName(),
                metadata.ipAddress(),
                metadata.userAgent(),
                Instant.now().toString()
        );
        return response;
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        try {
            RefreshTokenSession session = consumeRefreshTokenSession(refreshToken);

            String username = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(InvalidRefreshTokenException::new);
            UserDetails userDetails = customUserDetailsService.toUserDetails(user);

            if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
                throw new InvalidRefreshTokenException();
            }

            assertAccountStatus(user);

            if (!session.username().equals(user.getUsername())) {
                throw new InvalidRefreshTokenException();
            }

            return toAuthResponse(user, session.sessionId());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidRefreshTokenException();
        }
    }

    public void logout(RefreshTokenRequest request) {
        deleteRefreshToken(request.refreshToken());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = authValidator.normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(User::canAuthenticate).ifPresent(user -> {
            String token = passwordResetTokenService.issue(
                    user.getId(),
                    passwordResetProperties.tokenTtl()
            );

            try {
                String resetUrl = UriComponentsBuilder
                        .fromUriString(passwordResetProperties.frontendUrl())
                        .queryParam("token", token)
                        .build()
                        .encode()
                        .toUriString();
                authMailAsyncService.sendPasswordReset(
                        user.getEmail(),
                        user.getDisplayName(),
                        resetUrl,
                        token
                );
            } catch (RuntimeException ex) {
                passwordResetTokenService.invalidate(token);
                throw ex;
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        authValidator.validatePasswordConfirmation(request.newPassword(), request.confirmPassword());

        Long userId = passwordResetTokenService.consume(request.token());
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new PasswordReuseException();
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        deleteAllRefreshTokensForUser(userId);
        authMailAsyncService.sendPasswordChanged(
                user.getEmail(),
                user.getDisplayName(),
                Instant.now().toString()
        );
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        Long userId = emailVerificationTokenService.consume(request.token());
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidEmailVerificationTokenException::new);

        if (!user.isEmailVerified()) {
            user.markEmailVerified(Instant.now());
            userRepository.save(user);
        }
    }

    public void resendVerification(ResendVerificationRequest request) {
        String normalizedEmail = authValidator.normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerificationEmail);
    }

    @Transactional
    public AuthResponse changePassword(ChangePasswordRequest request) {
        authValidator.validatePasswordConfirmation(request.newPassword(), request.confirmPassword());

        User user = userRepository.findByUsernameIgnoreCase(currentUserProvider.getCurrentUsername())
                .orElseThrow(() -> new UnauthorizedException("user.current.not.found"));
        assertAccountStatus(user);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new PasswordReuseException();
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        deleteAllRefreshTokensForUser(user.getId());
        authMailAsyncService.sendPasswordChanged(
                user.getEmail(),
                user.getDisplayName(),
                Instant.now().toString()
        );

        return toAuthResponse(user);
    }

    private void sendVerificationEmail(User user) {
        String token = emailVerificationTokenService.createVerToken(user.getId(), emailVerificationProperties.tokenTtl());

        try {
            authMailAsyncService.sendEmailVerification(
                    user.getEmail(),
                    user.getDisplayName(),
                    buildUrl(emailVerificationProperties.frontendUrl(), token),
                    token
            );
        } catch (RuntimeException ex) {
            emailVerificationTokenService.invalidate(token);
            throw ex;
        }
    }

    private String buildUrl(String frontendUrl, String token) {
        return UriComponentsBuilder
                .fromUriString(frontendUrl)
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
    }

    private void checkUserRegisterUnique(String username, String email) {
        List<UserRepository.UserRegistrationConflict> conflicts =
                userRepository.findRegistrationConflicts(username, email);

        if (conflicts.stream().anyMatch(conflict -> conflict.getUsername().equalsIgnoreCase(username))) {
            throw new UsernameAlreadyExistsException();
        }
        if (conflicts.stream().anyMatch(conflict -> conflict.getEmail().equalsIgnoreCase(email))) {
            throw new EmailAlreadyExistsException();
        }
    }

    private AuthResponse toAuthResponse(User user) {
        return toAuthResponse(user, UUID.randomUUID().toString());
    }

    private AuthResponse toAuthResponse(User user, String sessionId) {
        UserDetails userDetails = customUserDetailsService.toUserDetails(user);

        String refreshToken = jwtService.generateRefreshToken(userDetails);
        long refreshTokenTime = jwtService.getRefreshTokenExpirationMs();
        RefreshTokenSession session = new RefreshTokenSession(
                user.getId(),
                user.getUsername(),
                sessionId
        );

        String hash = tokenHash.hashRefreshToken(refreshToken);
        setValueRedis(refreshTokenKey(hash), session, Duration.ofMillis(refreshTokenTime));

        indexRefreshToken(user.getId(), hash, Duration.ofMillis(refreshTokenTime));

        return authMapper.toResponse(
                user,
                jwtService.generateAccessToken(userDetails),
                refreshToken,
                jwtService.getAccessTokenExpirationMs(),
                refreshTokenTime
        );
    }

    private void assertAccountStatus(User user) {
        if (user.isLocked()) {
            throw new AccountLockedException();
        }

        if (!user.isActive()) {
            throw new AccountInactiveException();
        }

        if (!user.isEmailVerified()) {
            throw new EmailVerificationRequiredException();
        }
    }

    private void setValueRedis(String keyName, Object value, Duration timeToLive) {
        authValidator.validateRedisValue(keyName, value, timeToLive);

        try {
            redisTemplate.opsForValue().set(keyName, value, timeToLive);
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    private RefreshTokenSession consumeRefreshTokenSession(String refreshToken) {
        try {
            String hash = tokenHash.hashRefreshToken(refreshToken);
            Object value = redisTemplate.opsForValue().getAndDelete(refreshTokenKey(hash));
            if (value == null) {
                throw new InvalidRefreshTokenException();
            }

            if (!(value instanceof RefreshTokenSession session)) {
                throw new InvalidRefreshTokenException();
            }

            redisTemplate.opsForSet().remove(userRefreshTokensKey(session.userId()), hash);
            return session;
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    private void deleteRefreshToken(String refreshToken) {
        try {
            String hash = tokenHash.hashRefreshToken(refreshToken);
            Object value = redisTemplate.opsForValue().get(refreshTokenKey(hash));
            redisTemplate.delete(refreshTokenKey(hash));

            if (value instanceof RefreshTokenSession session) {
                redisTemplate.opsForSet().remove(userRefreshTokensKey(session.userId()), hash);
            }
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    private void indexRefreshToken(Long userId, String hash, Duration timeToLive) {
        try {
            String key = userRefreshTokensKey(userId);
            redisTemplate.opsForSet().add(key, hash);
            redisTemplate.expire(key, timeToLive);
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    private void deleteAllRefreshTokensForUser(Long userId) {
        try {
            String userKey = userRefreshTokensKey(userId);
            Set<Object> hashes = redisTemplate.opsForSet().members(userKey);

            if (hashes != null && !hashes.isEmpty()) {
                List<String> refreshKeys = hashes.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .sorted()
                        .map(this::refreshTokenKey)
                        .toList();
                if (!refreshKeys.isEmpty()) {
                    redisTemplate.delete(refreshKeys);
                }
            }

            redisTemplate.delete(userKey);
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    private String userRefreshTokensKey(Long userId) {
        return USER_REFRESH_TOKENS_KEY_PREFIX + userId;
    }

    private String refreshTokenKey(String hash) {
        return REFRESH_TOKEN_KEY_PREFIX + hash;
    }
}