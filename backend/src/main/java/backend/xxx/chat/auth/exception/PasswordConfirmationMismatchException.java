package backend.xxx.chat.auth.exception;

import java.util.List;

import backend.xxx.chat.common.dto.ApiFieldErrorResponse;
import backend.xxx.chat.common.exception.ValidationException;

public class PasswordConfirmationMismatchException extends ValidationException {

    public PasswordConfirmationMismatchException() {
        super(
                "Request validation failed",
                List.of(new ApiFieldErrorResponse("confirmPassword", "Confirm password does not match"))
        );
    }
}
