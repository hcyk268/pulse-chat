package backend.xxx.chat.realtime.model;

import backend.xxx.chat.message.dto.MessageResponse;

public record MessageCreatedEventData(
        MessageResponse message
) {
}

