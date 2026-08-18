package backend.xxx.chat.ai.client;

public record AiMedia(
        String contentType,
        String name,
        byte[] data
) {
}