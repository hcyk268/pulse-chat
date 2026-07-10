package backend.xxx.chat.outbox.dispatcher.impl;

import backend.xxx.chat.outbox.dispatcher.OutboxEventHandler;
import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.payload.MessageUnPinnedOutboxPayload;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.service.MessageRealtimeNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageUnPinnedOutboxHandler implements OutboxEventHandler {

    private final ObjectMapper objectMapper;
    private final MessageRealtimeNotifier notifier;

    @Override
    public boolean supports(String eventType) {
        return RealtimeEventType.MESSAGE_UNPINNED.getValue().equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        try {
            MessageUnPinnedOutboxPayload payload = objectMapper.readValue(
                    event.getPayload(),
                    MessageUnPinnedOutboxPayload.class
            );
            notifier.notifyUnPinned(event.getId(), payload.conversationId(), payload.messageId(), payload.unPinnedAt());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to handle message.unpinned", ex);
        }
    }
}
