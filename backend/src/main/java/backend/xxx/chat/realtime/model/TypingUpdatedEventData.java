package backend.xxx.chat.realtime.model;

public record TypingUpdatedEventData(
        Long userId,
        String username,
        boolean typing
) {
}
