package backend.xxx.chat.storage.dto;

import java.time.Instant;
import java.util.Map;

import backend.xxx.chat.storage.model.UploadPurpose;

public record PresignedUploadResponse(
        UploadPurpose purpose,
        String objectKey,
        String uploadUrl,
        String publicUrl,
        String method,
        Map<String, String> requiredHeaders,
        Instant expiresAt
) {
}
