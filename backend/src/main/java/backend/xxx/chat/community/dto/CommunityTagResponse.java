package backend.xxx.chat.community.dto;

import backend.xxx.chat.community.model.CommunityTag;

public record CommunityTagResponse(
        Long id,
        String slug,
        String name
) {

    public static CommunityTagResponse from(CommunityTag tag) {
        return new CommunityTagResponse(
                tag.getId(),
                tag.getSlug(),
                tag.getName()
        );
    }
}
