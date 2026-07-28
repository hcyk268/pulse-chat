package backend.xxx.chat.community.dto;

import backend.xxx.chat.storage.model.UploadedAsset;

public record CommunityAssetResponse(
        Long id,
        String publicUrl,
        String thumbnailUrl,
        String fileName,
        String contentType
) {

    public static CommunityAssetResponse from(UploadedAsset asset) {
        if (asset == null) {
            return null;
        }
        return new CommunityAssetResponse(
                asset.getId(),
                asset.getPublicUrl(),
                asset.getThumbnailUrl(),
                asset.getFileName(),
                asset.getContentType()
        );
    }
}
