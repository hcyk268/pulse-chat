package backend.xxx.chat.outbox.dispatcher.impl;

import backend.xxx.chat.outbox.dispatcher.OutboxDispatcher;
import backend.xxx.chat.outbox.dispatcher.OutboxEventHandler;
import backend.xxx.chat.outbox.model.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxDispatcherImpl implements OutboxDispatcher {

    private final List<OutboxEventHandler> handlers;

    @Override
    public void dispatch(OutboxEvent event) {
        OutboxEventHandler handler = handlers.stream()
                .filter(it -> it.supports(event.getEventType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported outbox event type: " + event.getEventType()
                ));
        handler.handle(event);
    }
}
