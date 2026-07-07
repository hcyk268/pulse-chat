package backend.xxx.chat.outbox.dispatcher;

import backend.xxx.chat.outbox.model.OutboxEvent;

public interface OutboxEventHandler {
    boolean supports(String eventType);
    void handle(OutboxEvent event);
}
