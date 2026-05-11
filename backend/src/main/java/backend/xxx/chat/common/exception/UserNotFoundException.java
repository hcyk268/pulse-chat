package backend.xxx.chat.common.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "User not found");
    }
}
