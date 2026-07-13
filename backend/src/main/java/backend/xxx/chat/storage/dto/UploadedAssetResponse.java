package backend.xxx.chat.storage.dto;

import backend.xxx.chat.storage.model.UploadPurpose;
import backend.xxx.chat.storage.model.UploadedAssetStatus;

public record UploadedAssetResponse(
        Long id,
        UploadPurpose purpose,
        String objectKey,
        String publicUrl,
        String fileName,
        String contentType,
        Long sizeBytes,
        UploadedAssetStatus status
) {
}