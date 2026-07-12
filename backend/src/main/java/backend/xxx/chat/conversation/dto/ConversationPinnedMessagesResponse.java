package backend.xxx.chat.conversation.dto;

import java.util.List;

import backend.xxx.chat.message.dto.MessagePinResponse;

public record ConversationPinnedMessagesResponse(
        Long conversationId,
        List<MessagePinResponse> items
) {
}
