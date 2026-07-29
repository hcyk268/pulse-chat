package backend.xxx.chat.auth.exception;

import backend.xxx.chat.common.exception.ValidationException;

public class InvalidCurrentPasswordException extends ValidationException {

    public InvalidCurrentPasswordException() {
        super("auth.password.current.invalid");
    }
}
