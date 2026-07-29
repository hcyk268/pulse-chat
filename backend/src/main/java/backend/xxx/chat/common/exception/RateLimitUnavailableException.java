package backend.xxx.chat.common.exception;

import org.springframework.http.HttpStatus;

public class RateLimitUnavailableException extends ApiException {

    public RateLimitUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE, "rate.limit.unavailable");
    }
}
