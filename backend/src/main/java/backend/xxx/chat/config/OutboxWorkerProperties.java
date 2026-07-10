package backend.xxx.chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.outbox.worker")
public class OutboxWorkerProperties {

    private boolean enabled = true;

    private long fixedDelayMs = 500;

    private long initialDelayMs = 1000;

    private int batchSize = 20;

    private int maxAttempts = 10;

    private Duration initialRetryDelay = Duration.ofSeconds(5);

    private Duration maxRetryDelay = Duration.ofMinutes(15);

    private Duration processingTimeout = Duration.ofSeconds(60);

    private int schedulerPoolSize = 1;

    public void setBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("app.outbox.worker.batch-size must be positive");
        }
        this.batchSize = batchSize;
    }

    public void setMaxAttempts(int maxAttempts) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("app.outbox.worker.max-attempts must be positive");
        }
        this.maxAttempts = maxAttempts;
    }

    public void setSchedulerPoolSize(int schedulerPoolSize) {
        if (schedulerPoolSize <= 0) {
            throw new IllegalArgumentException("app.outbox.worker.scheduler-pool-size must be positive");
        }
        this.schedulerPoolSize = schedulerPoolSize;
    }
}
