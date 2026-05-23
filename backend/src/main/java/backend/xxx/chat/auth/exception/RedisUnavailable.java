package backend.xxx.chat.auth.exception;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class RedisUnavailable extends ApiException {

    public RedisUnavailable() {
        super(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.INTERNAL_SERVER_ERROR, "Redis is temporarily unavailable");
    }
}
