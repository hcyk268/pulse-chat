package backend.xxx.chat.auth.exception;

import backend.xxx.chat.common.exception.ValidationException;

public class PasswordReuseException extends ValidationException {

    public PasswordReuseException() {
        super("auth.password.reuse.not.allowed");
    }
}
