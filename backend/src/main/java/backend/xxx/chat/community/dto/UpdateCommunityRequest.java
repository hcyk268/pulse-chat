package backend.xxx.chat.community.dto;

import java.util.List;

import backend.xxx.chat.community.model.CommunityVisibility;
import jakarta.validation.constraints.Size;

public record UpdateCommunityRequest(
        @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @Size(max = 80) String categorySlug,
        Long avatarAssetId,
        Long coverAssetId,
        CommunityVisibility visibility,
        List<@Size(max = 80) String> tagSlugs
) {
}
