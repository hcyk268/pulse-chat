package backend.xxx.chat.common.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class LimitExceedException extends ApiException {

    private final long retryAfterSeconds;

    public LimitExceedException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_REQUESTS, "rate.limit.exceeded");
        this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
    }
}
