package backend.xxx.chat.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompleteUploadPartRequest(
        @NotBlank String etag,
        @NotNull @Positive Long sizeBytes
) {
}