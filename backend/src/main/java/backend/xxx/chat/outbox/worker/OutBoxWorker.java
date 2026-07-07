package backend.xxx.chat.outbox.worker;

import backend.xxx.chat.outbox.service.OutboxProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.outbox.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutBoxWorker {

    private final OutboxProcessingService outboxProcessingService;

    @Scheduled(
            fixedDelayString = "500",
            initialDelayString = "1000",
            scheduler = "outboxTaskScheduler"
    )
    public void process() {
        int processedCount = outboxProcessingService.processBatch();
        if (processedCount > 0) {
            log.debug("Processed {} outbox event(s)", processedCount);
        }
    }
}
