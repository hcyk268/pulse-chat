package backend.xxx.chat.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AttachmentRequest(
        @NotBlank String objectKey,
        String url,
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotNull @Positive Long sizeBytes,
        @Positive Integer width,
        @Positive Integer height,
        @Positive Integer durationSeconds,
        String thumbnailUrl
) {
}
