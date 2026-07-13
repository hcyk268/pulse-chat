package backend.xxx.chat.storage.dto;

import java.time.Instant;
import java.util.Map;

public record PresignedUploadPartResponse(
        Long sessionId,
        Integer partNumber,
        String uploadUrl,
        String method,
        Map<String, String> requiredHeaders,
        Instant expiresAt
) {
}