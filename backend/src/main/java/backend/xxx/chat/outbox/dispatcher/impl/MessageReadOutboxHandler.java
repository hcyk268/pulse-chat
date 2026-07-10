package backend.xxx.chat.outbox.dispatcher.impl;

import backend.xxx.chat.outbox.dispatcher.OutboxEventHandler;
import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.payload.MessageReadOutboxPayload;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.service.MessageRealtimeNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageReadOutboxHandler implements OutboxEventHandler {

    private final ObjectMapper objectMapper;
    private final MessageRealtimeNotifier notifier;

    @Override
    public boolean supports(String eventType) {
        return RealtimeEventType.MESSAGE_READ.getValue().equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        try {
            MessageReadOutboxPayload payload = objectMapper.readValue(event.getPayload(), MessageReadOutboxPayload.class);
            notifier.notifyRead(
                    event.getId(),
                    payload.conversationId(),
                    payload.readerId(),
                    payload.lastReadMessageId(),
                    payload.readAt()
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to handle message.read", ex);
        }
    }
}
