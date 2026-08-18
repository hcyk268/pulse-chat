package backend.xxx.chat.ai.attachment;

public record LoadedAiAttachment(
        Long assetId,
        Long messageId,
        Long conversationId,
        Long ownerId,
        String objectKey,
        String fileName,
        String contentType,
        long sizeBytes,
        byte[] data
) {
    public boolean image() {
        return contentType != null && contentType.toLowerCase().startsWith("image/");
    }
}