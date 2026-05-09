package backend.xxx.chat.auth.service;

import backend.xxx.chat.auth.dto.AuthResponse;
import backend.xxx.chat.user.model.User;
import backend.xxx.chat.user.service.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthMapper {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserMapper userMapper;

    public AuthResponse toResponse(User user,
                                   String accessToken,
                                   String refreshToken,
                                   long accessTokenExpiresInMs,
                                   long refreshTokenExpiresInMs) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                accessTokenExpiresInMs,
                refreshTokenExpiresInMs,
                userMapper.toResponse(user)
        );
    }
}
