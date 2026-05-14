package backend.xxx.chat.realtime.model;

import backend.xxx.chat.conversation.dto.ConversationResponse;

public record ConversationUpdatedEventData(
        ConversationResponse conversation
) {
}