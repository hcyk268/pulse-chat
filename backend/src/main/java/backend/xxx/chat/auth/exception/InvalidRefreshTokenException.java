package backend.xxx.chat.auth.exception;

import backend.xxx.chat.common.exception.UnauthorizedException;

public class InvalidRefreshTokenException extends UnauthorizedException {

    public InvalidRefreshTokenException() {
        super("auth.invalid.refresh.token");
    }
}
