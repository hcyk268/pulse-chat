package backend.xxx.chat.user.dto;

import backend.xxx.chat.common.dto.CursorPageResponse;

import java.util.List;

public record UserSearchResponse(
        List<UserSearchItemResponse> items,
        CursorPageResponse paging
) {
}
