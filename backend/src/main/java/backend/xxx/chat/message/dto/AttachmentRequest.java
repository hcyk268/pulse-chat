package backend.xxx.chat.message.dto;

import jakarta.validation.constraints.Positive;

public record AttachmentRequest(
        @Positive Long assetId,
        String objectKey,
        String url,
        String fileName,
        String contentType,
        @Positive Long sizeBytes,
        @Positive Integer width,
        @Positive Integer height,
        @Positive Integer durationSeconds,
        String thumbnailUrl
) {
    public AttachmentRequest(
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
        this(null, objectKey, url, fileName, contentType, sizeBytes, width, height, durationSeconds, thumbnailUrl);
    }
}