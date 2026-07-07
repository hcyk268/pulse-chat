package backend.xxx.chat.outbox.service;

import backend.xxx.chat.outbox.dispatcher.OutboxDispatcher;
import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.model.OutboxStatus;
import backend.xxx.chat.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class OutboxProcessingService {

    private static final int BATCH_SIZE = 20;
    private static final long RETRY_DELAY_SECOND = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxDispatcher outboxDispatcher;


    @Transactional
    public int processBatch() {
        List<Long> eventIds = claimDispatchableEventIds();
        for (Long eventId : eventIds) {
            processSingleEvent(eventId);
        }
        return eventIds.size();
    }

    @Transactional
    protected List<Long> claimDispatchableEventIds() {
        List<OutboxEvent> events = outboxEventRepository.findDispatchableEvents(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                Instant.now(),
                PageRequest.of(0, BATCH_SIZE)
        );

        events.forEach(OutboxEvent::markProcessing);
        outboxEventRepository.saveAll(events);

        return events.stream()
                .map(OutboxEvent::getId)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processSingleEvent(Long eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();

        try {
            outboxDispatcher.dispatch(event);
            event.markDone(Instant.now());
        } catch (Exception ex) {
            event.markFailed(ex.getMessage(), Instant.now().plusSeconds(RETRY_DELAY_SECOND));
        }

        outboxEventRepository.save(event);
    }
}
