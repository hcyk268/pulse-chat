package backend.xxx.chat.auth.exception;

import backend.xxx.chat.common.exception.ValidationException;

public class InvalidEmailVerificationTokenException extends ValidationException {

    public InvalidEmailVerificationTokenException() {
        super("auth.email.verification.token.invalid");
    }
}
