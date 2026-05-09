package backend.xxx.chat.common.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path,
        List<ApiFieldErrorResponse> fieldErrors
) {
}
