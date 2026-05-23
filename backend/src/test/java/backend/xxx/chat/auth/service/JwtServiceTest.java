package backend.xxx.chat.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                jwtService,
                "accessTokenSecret",
                "change-me-access-token-secret-at-least-32-characters"
        );
        ReflectionTestUtils.setField(
                jwtService,
                "refreshTokenSecret",
                "change-me-refresh-token-secret-at-least-32-characters"
        );
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 86_400_000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", 864_000_000L);
    }

    @Test
    void generateAndValidateTokensWithPlainTextSecrets() {
        UserDetails userDetails = User.withUsername("adminchatapp")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        assertThat(jwtService.isAccessTokenValid(accessToken, userDetails)).isTrue();
        assertThat(jwtService.isRefreshTokenValid(refreshToken, userDetails)).isTrue();
    }

    @Test
    void accessTokenIsInvalidWhenUserIsDisabled() {
        UserDetails activeUser = User.withUsername("adminchatapp")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        UserDetails disabledUser = User.withUsername("adminchatapp")
                .password("password")
                .authorities("ROLE_USER")
                .disabled(true)
                .build();

        String accessToken = jwtService.generateAccessToken(activeUser);

        assertThat(jwtService.isAccessTokenValid(accessToken, disabledUser)).isFalse();
    }

    @Test
    void accessTokenIsInvalidWhenUserIsLocked() {
        UserDetails activeUser = User.withUsername("adminchatapp")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        UserDetails lockedUser = User.withUsername("adminchatapp")
                .password("password")
                .authorities("ROLE_USER")
                .accountLocked(true)
                .build();

        String accessToken = jwtService.generateAccessToken(activeUser);

        assertThat(jwtService.isAccessTokenValid(accessToken, lockedUser)).isFalse();
    }
}
