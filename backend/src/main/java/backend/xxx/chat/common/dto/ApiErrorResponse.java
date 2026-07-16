package backend.xxx.chat.common.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import backend.xxx.chat.common.web.Translator;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path,
        List<ApiFieldErrorResponse> fieldErrors
) {
    public ApiErrorResponse {
        message = Translator.toLocale(message);
    }
}
