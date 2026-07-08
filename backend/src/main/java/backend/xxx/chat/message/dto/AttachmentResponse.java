package backend.xxx.chat.message.dto;

public record AttachmentResponse(
        String objectKey,
        String url,
        String fileName,
        String contentType,
        Long sizeBytes,
        Integer width,
        Integer height,
        Integer durationSeconds,
        String thumbnailUrl
) {
}
