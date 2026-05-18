package backend.xxx.chat.message.dto;

import java.util.List;

import backend.xxx.chat.message.model.MessageReactionEmoji;
import backend.xxx.chat.user.dto.SummarizeUserResponse;

public record MessageReactionGroupResponse(
        MessageReactionEmoji emoji,
        long count,
        boolean reactedByMe,
        List<SummarizeUserResponse> users
) {
}
