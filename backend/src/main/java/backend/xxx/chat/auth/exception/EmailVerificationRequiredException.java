package backend.xxx.chat.auth.exception;

import backend.xxx.chat.common.exception.ApiException;
import backend.xxx.chat.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class EmailVerificationRequiredException extends ApiException {

    public EmailVerificationRequiredException() {
        super(HttpStatus.FORBIDDEN, ErrorCode.EMAIL_VERIFICATION_REQUIRED, "auth.email.verification.required");
    }
}
