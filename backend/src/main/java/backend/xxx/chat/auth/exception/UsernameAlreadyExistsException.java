package backend.xxx.chat.auth.exception;

import java.util.List;

import backend.xxx.chat.common.dto.ApiFieldErrorResponse;
import backend.xxx.chat.common.exception.ConflictException;

public class UsernameAlreadyExistsException extends ConflictException {

    public UsernameAlreadyExistsException() {
        super(
                "user.exists",
                List.of(new ApiFieldErrorResponse("username", "user.username.exists"))
        );
    }
}
