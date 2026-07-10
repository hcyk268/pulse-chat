package backend.xxx.chat.outbox.service;

import backend.xxx.chat.config.OutboxWorkerProperties;
import backend.xxx.chat.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventClaimer {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxWorkerProperties properties;

    private final String workerId = buildWorkerId();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> claimBatch() {
        Instant now = Instant.now();

        return outboxEventRepository.claimDispatchableEventIds(
                now,
                properties.getBatchSize(),
                workerId
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int resetStuckProcessingEvents() {
        Instant now = Instant.now();
        Instant timeoutBefore = now.minus(properties.getProcessingTimeout());

        return outboxEventRepository.resetStuckProcessingEvents(
                timeoutBefore,
                now
        );
    }

    private String buildWorkerId() {
        String host = "unknown-host";

        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            log.warn("Could not resolve hostname for outbox worker id", ex);
        }

        return host + "-" + UUID.randomUUID();
    }
}
