package backend.xxx.chat.ai.orchestration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import backend.xxx.chat.common.exception.ServiceUnavailableException;

public final class AiExecutionBudget {

    private final int maxProviderCalls;
    private final AtomicInteger providerCalls = new AtomicInteger();
    private final long deadlineNanos;

    public AiExecutionBudget(int maxProviderCalls, Duration workflowTimeout) {
        if (maxProviderCalls <= 0) {
            throw new IllegalArgumentException("maxProviderCalls must be positive");
        }
        if (workflowTimeout == null || workflowTimeout.isZero() || workflowTimeout.isNegative()) {
            throw new IllegalArgumentException("workflowTimeout must be positive");
        }
        this.maxProviderCalls = maxProviderCalls;
        this.deadlineNanos = System.nanoTime() + workflowTimeout.toNanos();
    }

    public int acquireProviderCall() {
        if (deadlineReached()) {
            throw new ServiceUnavailableException("ai.request.deadline.exceeded");
        }
        int current = providerCalls.incrementAndGet();
        if (current > maxProviderCalls) {
            providerCalls.decrementAndGet();
            throw new ServiceUnavailableException("ai.request.call.limit.exceeded");
        }
        return current;
    }

    public boolean deadlineReached() {
        return System.nanoTime() >= deadlineNanos;
    }
}
