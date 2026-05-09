package backend.xxx.chat.auth.exception;

import java.util.List;

import backend.xxx.chat.common.dto.ApiFieldErrorResponse;
import backend.xxx.chat.common.exception.ConflictException;

public class UsernameAlreadyExistsException extends ConflictException {

    public UsernameAlreadyExistsException() {
        super(
                "User already exists",
                List.of(new ApiFieldErrorResponse("username", "Username already exists"))
        );
    }
}
