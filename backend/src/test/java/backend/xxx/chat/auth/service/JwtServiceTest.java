package backend.xxx.chat.auth.service;

import java.util.Base64;
import java.util.List;

import backend.xxx.chat.auth.model.AuthenticatedUser;
import backend.xxx.chat.user.model.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "accessTokenSecret", secret((byte) 1));
        ReflectionTestUtils.setField(jwtService, "refreshTokenSecret", secret((byte) 2));
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 60_000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", 120_000L);
    }

    @Test
    void rejectsStaleToken() {
        AuthenticatedUser original = principal(3L, true);
        String token = jwtService.generateAccessToken(original);

        assertThat(jwtService.isAccessTokenValid(token, original)).isTrue();
        assertThat(jwtService.isAccessTokenValid(token, principal(4L, true))).isFalse();
    }

    @Test
    void rejectsUnverifiedUser() {
        AuthenticatedUser verified = principal(0L, true);
        String token = jwtService.generateAccessToken(verified);

        assertThat(jwtService.isAccessTokenValid(token, principal(0L, false))).isFalse();
    }

    private AuthenticatedUser principal(long credentialsVersion, boolean emailVerified) {
        return new AuthenticatedUser(
                10L,
                "alice",
                "hashed-password",
                AccountStatus.ACTIVE,
                emailVerified,
                credentialsVersion,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private String secret(byte value) {
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, value);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
