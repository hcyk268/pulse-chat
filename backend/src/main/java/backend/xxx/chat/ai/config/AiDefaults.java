package backend.xxx.chat.ai.config;

import java.time.Duration;

public final class AiDefaults {

    public static final int MAX_RETRIES = 2;
    public static final Duration RETRY_INITIAL_BACKOFF = Duration.ofMillis(250);
    public static final Duration RETRY_MAX_BACKOFF = Duration.ofSeconds(2);
    public static final int MAX_CONCURRENT_REQUESTS = 20;
    public static final int MAX_PROVIDER_CALLS_PER_REQUEST = 8;
    public static final Duration WORKFLOW_TIMEOUT = Duration.ofSeconds(90);
    public static final int MAX_TOOL_CALLS = 5;
    public static final int MAX_STEPS = 8;
    public static final int MAX_INPUT_CHARS = 12_000;

    private AiDefaults() {
    }
}
