package backend.xxx.chat.auth.service;

import java.util.Locale;

import backend.xxx.chat.auth.dto.AuthResponse;
import backend.xxx.chat.auth.dto.LoginRequest;
import backend.xxx.chat.auth.dto.RefreshTokenRequest;
import backend.xxx.chat.auth.dto.RegisterRequest;
import backend.xxx.chat.auth.exception.EmailAlreadyExistsException;
import backend.xxx.chat.auth.exception.InvalidRefreshTokenException;
import backend.xxx.chat.auth.exception.PasswordConfirmationMismatchException;
import backend.xxx.chat.auth.exception.UsernameAlreadyExistsException;
import backend.xxx.chat.common.exception.AccountInactiveException;
import backend.xxx.chat.common.exception.AccountLockedException;
import backend.xxx.chat.common.exception.UnauthorizedException;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final AuthMapper authMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validatePasswordConfirmation(request.password(), request.confirmPassword());

        String normalizedUsername = normalizeUsername(request.username());
        String normalizedEmail = normalizeEmail(request.email());

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
            String username = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(InvalidRefreshTokenException::new);
            UserDetails userDetails = customUserDetailsService.toUserDetails(user);

            if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
                throw new InvalidRefreshTokenException();
            }

            assertAccountStatus(user);
            return toAuthResponse(user);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidRefreshTokenException();
        }
    }

    private void validatePasswordConfirmation(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new PasswordConfirmationMismatchException();
        }
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
        UserDetails userDetails = customUserDetailsService.toUserDetails(user);
        return authMapper.toResponse(
                user,
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails),
                jwtService.getAccessTokenExpirationMs(),
                jwtService.getRefreshTokenExpirationMs()
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

    private String normalizeUsername(String username) {
        return username.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
