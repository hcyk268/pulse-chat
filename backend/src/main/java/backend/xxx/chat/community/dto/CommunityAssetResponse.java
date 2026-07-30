package backend.xxx.chat.community.dto;

public record CommunityAssetResponse(
        Long id,
        String publicUrl,
        String thumbnailUrl,
        String fileName,
        String contentType
) {
}
