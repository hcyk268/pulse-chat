package backend.xxx.chat.conversation.dto;

import java.util.List;

import backend.xxx.chat.message.dto.MessagePinResponse;

public record ConversationPinsResponse(
        Long conversationId,
        List<MessagePinResponse> items
) {
}
