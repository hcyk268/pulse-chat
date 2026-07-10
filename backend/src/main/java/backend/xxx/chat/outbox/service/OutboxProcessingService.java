package backend.xxx.chat.outbox.service;

import backend.xxx.chat.outbox.dispatcher.OutboxDispatcher;
import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.model.OutboxStatus;
import backend.xxx.chat.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxProcessingService {

    private final OutboxEventClaimer outboxEventClaimer;
    private final OutboxSingleEventProcessor outboxSingleEventProcessor;

    public int processBatch() {
        int recoveredCount = outboxEventClaimer.resetStuckProcessingEvents();

        if (recoveredCount > 0) {
            log.warn("Recovered {} stuck PROCESSING outbox event(s)", recoveredCount);
        }

        List<Long> eventIds = outboxEventClaimer.claimBatch();

        for (Long eventId : eventIds) {
            outboxSingleEventProcessor.process(eventId);
        }

        return eventIds.size();
    }
}
