package backend.xxx.chat.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import backend.xxx.chat.auth.model.JwtTokenType;
import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Getter
@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    @Value("${jwt.access-token-secret}")
    private String accessTokenSecret;

    @Value("${jwt.refresh-token-secret}")
    private String refreshTokenSecret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(Map.of(), userDetails);
    }

    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return generateToken(extraClaims, userDetails, accessTokenExpirationMs, JwtTokenType.ACCESS);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateRefreshToken(Map.of(), userDetails);
    }

    public String generateRefreshToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return generateToken(extraClaims, userDetails, refreshTokenExpirationMs, JwtTokenType.REFRESH);
    }

    public JwtTokenType extractTokenType(String token) {
        String tokenType = extractAllClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
        return JwtTokenType.valueOf(tokenType);
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, JwtTokenType.ACCESS);
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, JwtTokenType.REFRESH);
    }

    private String generateToken(Map<String, Object> extraClaims,
                                 UserDetails userDetails,
                                 long expirationMs,
                                 JwtTokenType tokenType) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(tokenType))
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    private boolean isTokenValid(String token, UserDetails userDetails, JwtTokenType expectedTokenType) {
        String username = extractUsername(token);
        JwtTokenType tokenType = extractTokenType(token);
        return username.equals(userDetails.getUsername())
                && tokenType == expectedTokenType
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        JwtTokenType tokenType = extractUnsignedTokenType(token);
        return Jwts.parser()
                .verifyWith(getSigningKey(tokenType))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private JwtTokenType extractUnsignedTokenType(String token) {
        Claims claims = Jwts.parser()
                .unsecured()
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        return JwtTokenType.valueOf(tokenType);
    }

    private SecretKey getSigningKey(JwtTokenType tokenType) {
        String secret = tokenType == JwtTokenType.ACCESS ? accessTokenSecret : refreshTokenSecret;
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException ex) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }
}
