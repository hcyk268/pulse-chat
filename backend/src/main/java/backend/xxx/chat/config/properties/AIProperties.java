package backend.xxx.chat.config.properties;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai")
public class AIProperties {

    private String baseUrl = "";

    private String apiKey = "";

    private String model = "";

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration timeout = Duration.ofSeconds(30);

    private int maxOutputTokens = 800;

    private double temperature = 0.2;

    private boolean nativeStructuredOutput = true;

    private Attachment attachment = new Attachment();

    private Memory memory = new Memory();

    private RateLimit rateLimit = new RateLimit();

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = requirePositive(connectTimeout, "app.ai.connect-timeout");
    }

    public void setTimeout(Duration timeout) {
        this.timeout = requirePositive(timeout, "app.ai.timeout");
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        if (maxOutputTokens <= 0 || maxOutputTokens > 8_000) {
            throw new IllegalArgumentException("app.ai.max-output-tokens must be between 1 and 8000");
        }
        this.maxOutputTokens = maxOutputTokens;
    }

    public void setTemperature(double temperature) {
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("app.ai.temperature must be between 0 and 2");
        }
        this.temperature = temperature;
    }

    private Duration requirePositive(Duration duration, String propertyName) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be positive");
        }
        return duration;
    }

    @Getter
    @Setter
    public static class Attachment {

        private int maxFiles = 5;

        private long maxImageBytes = 5 * 1024 * 1024L;

        private long maxDocumentBytes = 10 * 1024 * 1024L;

        private int maxExtractedChars = 12_000;

        public void setMaxFiles(int maxFiles) {
            if (maxFiles <= 0 || maxFiles > 10) {
                throw new IllegalArgumentException("app.ai.attachment.max-files must be between 1 and 10");
            }
            this.maxFiles = maxFiles;
        }

        public void setMaxImageBytes(long maxImageBytes) {
            this.maxImageBytes = requirePositiveBytes(maxImageBytes, "app.ai.attachment.max-image-bytes");
        }

        public void setMaxDocumentBytes(long maxDocumentBytes) {
            this.maxDocumentBytes = requirePositiveBytes(maxDocumentBytes, "app.ai.attachment.max-document-bytes");
        }

        public void setMaxExtractedChars(int maxExtractedChars) {
            if (maxExtractedChars <= 0) {
                throw new IllegalArgumentException("app.ai.attachment.max-extracted-chars must be positive");
            }
            this.maxExtractedChars = maxExtractedChars;
        }

        private long requirePositiveBytes(long value, String propertyName) {
            if (value <= 0) {
                throw new IllegalArgumentException(propertyName + " must be positive");
            }
            return value;
        }
    }

    @Getter
    @Setter
    public static class Memory {

        private boolean enabled = true;

        private int maxMessages = 12;

        private Duration ttl = Duration.ofDays(1);

        public void setMaxMessages(int maxMessages) {
            if (maxMessages <= 0 || maxMessages > 50) {
                throw new IllegalArgumentException("app.ai.memory.max-messages must be between 1 and 50");
            }
            this.maxMessages = maxMessages;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl == null ? Duration.ofDays(1) : ttl;
        }
    }

    @Getter
    @Setter
    public static class RateLimit {

        private int maxRequests = 20;

        private int globalMaxRequests = 30;

        private Duration window = Duration.ofMinutes(1);

        public void setMaxRequests(int maxRequests) {
            if (maxRequests <= 0) {
                throw new IllegalArgumentException("app.ai.rate-limit.max-requests must be positive");
            }
            this.maxRequests = maxRequests;
        }

        public void setGlobalMaxRequests(int globalMaxRequests) {
            if (globalMaxRequests <= 0) {
                throw new IllegalArgumentException("app.ai.rate-limit.global-max-requests must be positive");
            }
            this.globalMaxRequests = globalMaxRequests;
        }

        public void setWindow(Duration window) {
            if (window == null || window.isZero() || window.isNegative()) {
                throw new IllegalArgumentException("app.ai.rate-limit.window must be positive");
            }
            this.window = window;
        }
    }
}