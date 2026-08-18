package backend.xxx.chat.ai.attachment;

public record AiAttachmentSource(
        Long assetId,
        Long messageId,
        Long conversationId,
        String fileName,
        String contentType,
        String sourceType
) {
}