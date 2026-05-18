package backend.xxx.chat.message.dto;

import java.util.List;

public record MessageReactionsResponse(
        Long messageId,
        List<MessageReactionGroupResponse> items
) {
}
