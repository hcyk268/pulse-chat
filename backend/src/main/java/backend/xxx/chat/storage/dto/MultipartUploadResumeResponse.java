package backend.xxx.chat.storage.dto;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.storage.model.UploadSessionStatus;

public record MultipartUploadResumeResponse(
        Long sessionId,
        UploadSessionStatus status,
        Integer totalParts,
        Long chunkSizeBytes,
        List<Integer> uploadedParts,
        List<Integer> missingParts,
        Instant expiresAt
) {
}