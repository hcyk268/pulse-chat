package backend.xxx.chat.auth.service;

import java.time.Duration;
import java.util.UUID;

import backend.xxx.chat.auth.dto.AuthResponse;
import backend.xxx.chat.auth.dto.LoginRequest;
import backend.xxx.chat.auth.dto.RefreshTokenRequest;
import backend.xxx.chat.auth.dto.RegisterRequest;
import backend.xxx.chat.auth.exception.EmailAlreadyExistsException;
import backend.xxx.chat.auth.exception.InvalidRefreshTokenException;
import backend.xxx.chat.auth.exception.RedisUnavailable;
import backend.xxx.chat.auth.exception.UsernameAlreadyExistsException;
import backend.xxx.chat.auth.model.RefreshTokenSession;
import backend.xxx.chat.common.exception.AccountInactiveException;
import backend.xxx.chat.common.exception.AccountLockedException;
import backend.xxx.chat.common.exception.UnauthorizedException;
import backend.xxx.chat.common.util.TokenHash;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenHash tokenHash;
    private final AuthValidator authValidator;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
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

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password())
        );

        String username = authentication.getName();
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid username/email or password"));

        assertAccountStatus(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        try {
            RefreshTokenSession session = getRefreshTokenSession(refreshToken);

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

            deleteRefreshToken(refreshToken);
            return toAuthResponse(user, session.sessionId());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidRefreshTokenException();
        }
    }

    public void logout(RefreshTokenRequest request) {
        deleteRefreshToken(request.refreshToken());
    }

    private void checkUserRegisterUnique(String username, String email) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyExistsException();
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
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

        return authMapper.toResponse(
                user,
                jwtService.generateAccessToken(userDetails),
                refreshToken,
                jwtService.getAccessTokenExpirationMs(),
                refreshTokenTime
        );
    }

    private void assertAccountStatus(User user) {
        if (!user.canAuthenticate()) {
            throw new AccountInactiveException();
        }

        if (user.isLocked()) {
            throw new AccountLockedException();
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

    private RefreshTokenSession getRefreshTokenSession(String refreshToken) {
        try {
            String hash = tokenHash.hashRefreshToken(refreshToken);
            Object value = redisTemplate.opsForValue().get(refreshTokenKey(hash));
            if (value == null) {
                throw new InvalidRefreshTokenException();
            }

            if (!(value instanceof RefreshTokenSession session)) {
                throw new InvalidRefreshTokenException();
            }

            return session;
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    private void deleteRefreshToken(String refreshToken) {
        try {
            String hash = tokenHash.hashRefreshToken(refreshToken);
            redisTemplate.delete(refreshTokenKey(hash));
        } catch (DataAccessException ex) {
            throw new RedisUnavailable();
        }
    }

    private String refreshTokenKey(String hash) {
        return REFRESH_TOKEN_KEY_PREFIX + hash;
    }
}