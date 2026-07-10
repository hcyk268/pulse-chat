package backend.xxx.chat.outbox.service;

import backend.xxx.chat.config.OutboxWorkerProperties;
import backend.xxx.chat.outbox.dispatcher.OutboxDispatcher;
import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.model.OutboxStatus;
import backend.xxx.chat.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxSingleEventProcessor {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxDispatcher outboxDispatcher;
    private final OutboxWorkerProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);

        if (event == null) {
            log.warn("Outbox event {} was claimed but no longer exists", eventId);
            return;
        }

        if (event.getStatus() != OutboxStatus.PROCESSING) {
            log.debug(
                    "Skip outbox event {} because status is {}",
                    event.getId(),
                    event.getStatus()
            );
            return;
        }

        try {
            outboxDispatcher.dispatch(event);
            event.markDone(Instant.now());
        } catch (Exception ex) {
            markFailure(event, ex);
        }

        outboxEventRepository.save(event);
    }

    private void markFailure(OutboxEvent event, Exception ex) {
        Instant now = Instant.now();
        int nextAttemptCount = event.getAttemptCount() + 1;
        String errorMessage = buildErrorMessage(ex);

        if (nextAttemptCount >= properties.getMaxAttempts()) {
            event.markDead(errorMessage, now);

            log.error(
                    "Outbox event {} moved to DEAD after {} attempt(s). eventType={}, aggregateType={}, aggregateId={}",
                    event.getId(),
                    nextAttemptCount,
                    event.getEventType(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    ex
            );

            return;
        }

        Instant nextAttemptAt = calculateNextAttemptAt(now, nextAttemptCount);
        event.markFailed(errorMessage, nextAttemptAt);

        log.warn(
                "Outbox event {} failed on attempt {}/{}. Retry at {}. eventType={}, aggregateType={}, aggregateId={}",
                event.getId(),
                nextAttemptCount,
                properties.getMaxAttempts(),
                nextAttemptAt,
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                ex
        );
    }

    private Instant calculateNextAttemptAt(Instant now, int nextAttemptCount) {
        Duration initialDelay = properties.getInitialRetryDelay();
        Duration maxDelay = properties.getMaxRetryDelay();

        long initialDelayMillis = Math.max(1, initialDelay.toMillis());
        long maxDelayMillis = Math.max(initialDelayMillis, maxDelay.toMillis());

        int exponent = Math.max(0, nextAttemptCount - 1);
        long multiplier = 1L << Math.min(exponent, 20);

        long delayMillis = Math.min(initialDelayMillis * multiplier, maxDelayMillis);

        return now.plusMillis(delayMillis);
    }

    private String buildErrorMessage(Exception ex) {
        Throwable root = ex;

        while (root.getCause() != null) {
            root = root.getCause();
        }

        String rootMessage = root.getMessage();
        if (rootMessage == null || rootMessage.isBlank()) {
            return root.getClass().getSimpleName();
        }

        return root.getClass().getSimpleName() + ": " + rootMessage;
    }
}
