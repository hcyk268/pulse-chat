package backend.xxx.chat.common.exception;

import java.util.List;

import backend.xxx.chat.common.dto.ApiFieldErrorResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;
    private final List<ApiFieldErrorResponse> fieldErrors;

    public ApiException(HttpStatus status, ErrorCode code, String message) {
        this(status, code, message, List.of());
    }

    public ApiException(HttpStatus status, ErrorCode code, String message, List<ApiFieldErrorResponse> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors;
    }

}
