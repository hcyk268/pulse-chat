package backend.xxx.chat.conversation.dto;

import backend.xxx.chat.common.dto.CursorPageResponse;

import java.util.List;

public record ConversationBoxResponse(
        List<ConversationResponse> items,
        CursorPageResponse paging
) {
}
