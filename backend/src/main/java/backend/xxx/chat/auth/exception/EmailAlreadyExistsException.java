package backend.xxx.chat.auth.exception;

import java.util.List;

import backend.xxx.chat.common.dto.ApiFieldErrorResponse;
import backend.xxx.chat.common.exception.ConflictException;

public class EmailAlreadyExistsException extends ConflictException {

    public EmailAlreadyExistsException() {
        super(
                "User already exists",
                List.of(new ApiFieldErrorResponse("email", "Email already exists"))
        );
    }
}
