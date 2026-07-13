package backend.xxx.chat.storage.dto;

import java.time.Instant;

import backend.xxx.chat.storage.model.UploadPurpose;
import backend.xxx.chat.storage.model.UploadSessionStatus;

public record MultipartUploadSessionResponse(
        Long sessionId,
        UploadPurpose purpose,
        String objectKey,
        String fileName,
        String contentType,
        Long sizeBytes,
        Long chunkSizeBytes,
        Integer totalParts,
        UploadSessionStatus status,
        Instant expiresAt
) {
}