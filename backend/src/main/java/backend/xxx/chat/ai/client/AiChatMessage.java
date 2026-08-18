package backend.xxx.chat.ai.client;

import java.util.List;

public record AiChatMessage(
        String role,
        String content,
        List<AiMedia> media
) {

    public AiChatMessage(String role, String content) {
        this(role, content, List.of());
    }

    public static AiChatMessage system(String content) {
        return new AiChatMessage("system", content);
    }

    public static AiChatMessage user(String content) {
        return new AiChatMessage("user", content);
    }

    public static AiChatMessage user(String content, List<AiMedia> media) {
        return new AiChatMessage("user", content, media == null ? List.of() : List.copyOf(media));
    }

    public static AiChatMessage assistant(String content) {
        return new AiChatMessage("assistant", content);
    }
}