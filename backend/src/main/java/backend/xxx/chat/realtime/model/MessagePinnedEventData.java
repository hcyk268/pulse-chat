package backend.xxx.chat.realtime.model;

import backend.xxx.chat.message.dto.MessagePinResponse;

public record MessagePinnedEventData(
        MessagePinResponse pin
) {
}
