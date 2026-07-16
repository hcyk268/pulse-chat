package backend.xxx.chat.common.exception;

import org.springframework.http.HttpStatus;

public class AccountInactiveException extends ApiException {

    public AccountInactiveException() {
        super(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_INACTIVE, "account.inactive");
    }
}
