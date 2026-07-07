package backend.xxx.chat.outbox.dispatcher;

import backend.xxx.chat.outbox.model.OutboxEvent;

public interface OutboxDispatcher {
    void dispatch(OutboxEvent event);
}