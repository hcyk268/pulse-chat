package backend.xxx.chat.common.exception;

import org.springframework.http.HttpStatus;

public class LimitExceedException extends ApiException {
    public LimitExceedException() {
        super(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_REQUESTS, "rate.limit.exceeded");
    }
}
