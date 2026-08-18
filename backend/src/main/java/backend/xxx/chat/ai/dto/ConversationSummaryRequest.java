package backend.xxx.chat.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ConversationSummaryRequest(
        @Min(1) @Max(50) Short limit
) {

    public short normalizedLimit() {
        return limit == null ? 50 : limit;
    }
}