package backend.xxx.chat.auth.exception;

import backend.xxx.chat.common.exception.ValidationException;

public class InvalidPasswordResetTokenException extends ValidationException {

    public InvalidPasswordResetTokenException() {
        super("auth.password.reset.token.invalid");
    }
}

