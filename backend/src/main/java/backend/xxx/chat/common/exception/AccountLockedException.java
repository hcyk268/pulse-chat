package backend.xxx.chat.common.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends ApiException {

    public AccountLockedException() {
        super(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_LOCKED, "account.locked");
    }
}
