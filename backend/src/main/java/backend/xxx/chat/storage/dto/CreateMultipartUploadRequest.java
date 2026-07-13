package backend.xxx.chat.storage.dto;

import backend.xxx.chat.storage.model.UploadPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMultipartUploadRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Size(max = 100) String contentType,
        @NotNull @Positive Long sizeBytes,
        @Positive Long chunkSizeBytes,
        @Size(max = 128) String fileChecksum,
        @NotNull UploadPurpose purpose
) {
}