package backend.xxx.chat.storage.service;

import backend.xxx.chat.storage.dto.MultipartUploadSessionResponse;
import backend.xxx.chat.storage.dto.UploadedAssetResponse;
import backend.xxx.chat.storage.model.UploadSession;
import backend.xxx.chat.storage.model.UploadedAsset;
import org.springframework.stereotype.Component;

@Component
public class UploadedAssetMapper {

    public MultipartUploadSessionResponse toSessionResponse(UploadSession session) {
        return new MultipartUploadSessionResponse(
                session.getId(),
                session.getPurpose(),
                session.getObjectKey(),
                session.getFileName(),
                session.getContentType(),
                session.getSizeBytes(),
                session.getChunkSizeBytes(),
                session.getTotalParts(),
                session.getStatus(),
                session.getExpiresAt()
        );
    }

    public UploadedAssetResponse toAssetResponse(UploadedAsset asset) {
        return new UploadedAssetResponse(
                asset.getId(),
                asset.getPurpose(),
                asset.getObjectKey(),
                asset.getPublicUrl(),
                asset.getFileName(),
                asset.getContentType(),
                asset.getSizeBytes(),
                asset.getStatus()
        );
    }
}