package backend.xxx.chat.common.dto;

import backend.xxx.chat.common.web.Translator;

public record ApiFieldErrorResponse(
        String field,
        String message
) {
    public ApiFieldErrorResponse {
        message = Translator.toLocale(message);
    }
}
