package backend.xxx.chat.auth.exception;

import backend.xxx.chat.common.exception.ServiceUnavailableException;

public class RedisUnavailable extends ServiceUnavailableException {

    public RedisUnavailable() {
        super("Redis is temporarily unavailable");
    }
}