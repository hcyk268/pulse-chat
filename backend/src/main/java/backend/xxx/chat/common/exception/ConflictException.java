package backend.xxx.chat.common.exception;

import java.util.List;

import backend.xxx.chat.common.dto.ApiFieldErrorResponse;
import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message);
    }

    public ConflictException(String message, List<ApiFieldErrorResponse> fieldErrors) {
        super(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message, fieldErrors);
    }
}
