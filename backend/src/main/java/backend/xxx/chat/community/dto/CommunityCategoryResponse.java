package backend.xxx.chat.community.dto;

import backend.xxx.chat.community.model.CommunityCategory;

public record CommunityCategoryResponse(
        Long id,
        String slug,
        String name,
        String description,
        int sortOrder
) {

    public static CommunityCategoryResponse from(CommunityCategory category) {
        return new CommunityCategoryResponse(
                category.getId(),
                category.getSlug(),
                category.getName(),
                category.getDescription(),
                category.getSortOrder()
        );
    }
}
