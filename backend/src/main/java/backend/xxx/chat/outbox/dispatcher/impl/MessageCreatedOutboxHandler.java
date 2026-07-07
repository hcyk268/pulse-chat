package backend.xxx.chat.outbox.dispatcher.impl;

import backend.xxx.chat.outbox.dispatcher.OutboxEventHandler;
import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.payload.MessageCreatedOutboxPayload;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.service.MessageRealtimeNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageCreatedOutboxHandler implements OutboxEventHandler {

    private final ObjectMapper objectMapper;
    private final MessageRealtimeNotifier notifier;

    @Override
    public boolean supports(String eventType) {
        return RealtimeEventType.MESSAGE_CREATED.getValue().equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        try {
            MessageCreatedOutboxPayload payload = objectMapper.readValue(
                    event.getPayload(),
                    MessageCreatedOutboxPayload.class
            );

            notifier.notifyCreated(payload.conversationId(), payload.messageId());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to handle message.created", ex);
        }
    }
}
