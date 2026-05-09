package backend.xxx.chat.common.dto;

public record CursorPageResponse(
        int limit,
        String nextCursor,
        boolean hasMore
) {
}
