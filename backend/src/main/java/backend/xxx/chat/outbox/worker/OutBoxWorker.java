package backend.xxx.chat.outbox.worker;

import backend.xxx.chat.outbox.service.OutboxProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutBoxWorker {

    private final OutboxProcessingService outboxProcessingService;

    @Scheduled(
            fixedDelayString = "${app.outbox.worker.fixed-delay-ms:500}",
            initialDelayString = "${app.outbox.worker.initial-delay-ms:1000}",
            scheduler = "outboxTaskScheduler"
    )
    public void process() {
        int processedCount = outboxProcessingService.processBatch();
        if (processedCount > 0) {
            log.debug("Processed {} outbox event", processedCount);
        }
    }
}
