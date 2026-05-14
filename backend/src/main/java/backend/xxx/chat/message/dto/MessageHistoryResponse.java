package backend.xxx.chat.message.dto;

import backend.xxx.chat.common.dto.CursorPageResponse;

import java.util.List;

public record MessageHistoryResponse(
        Long conversationId,
        List<MessageResponse> items,
        CursorPageResponse paging
) {
}
