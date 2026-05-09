package backend.xxx.chat.common.exception;

import java.util.List;

import backend.xxx.chat.common.dto.ApiFieldErrorResponse;
import org.springframework.http.HttpStatus;

public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message);
    }

    public ValidationException(String message, List<ApiFieldErrorResponse> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message, fieldErrors);
    }
}
