package backend.xxx.chat.community.dto;

public record CommunityCategoryResponse(
        Long id,
        String slug,
        String name,
        String description,
        int sortOrder
) {
}
