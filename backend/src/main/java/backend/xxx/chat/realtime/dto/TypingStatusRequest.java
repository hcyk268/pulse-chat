package backend.xxx.chat.realtime.dto;

import jakarta.validation.constraints.NotNull;

public record TypingStatusRequest(
        @NotNull Boolean typing
) {
}
