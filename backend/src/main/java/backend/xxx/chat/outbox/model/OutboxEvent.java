package backend.xxx.chat.outbox.model;

import java.time.Instant;
import java.util.Objects;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import backend.xxx.chat.common.web.Translator;

@Getter
@Entity
@Table(name = "outbox_events")
@NoArgsConstructor
public class OutboxEvent extends AbstractBaseEntity<Long> {

    private static final int AGGREGATE_TYPE_MAX_LENGTH = 50;
    private static final int EVENT_TYPE_MAX_LENGTH = 100;
    private static final int LAST_ERROR_MAX_LENGTH = 1000;

    @Column(name = "aggregate_type", nullable = false, length = AGGREGATE_TYPE_MAX_LENGTH)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = EVENT_TYPE_MAX_LENGTH)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error", length = LAST_ERROR_MAX_LENGTH)
    private String lastError;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "event_version", nullable = false)
    private int eventVersion = 1;

    public static OutboxEvent pending(
            String aggregateType,
            Long aggregateId,
            String eventType,
            String payload
    ) {
        return pending(aggregateType, aggregateId, eventType, payload, Instant.now());
    }

    public static OutboxEvent pending(
            String aggregateType,
            Long aggregateId,
            String eventType,
            String payload,
            Instant availableAt
    ) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateType = requireText(aggregateType, "aggregateType", AGGREGATE_TYPE_MAX_LENGTH);
        event.aggregateId = Objects.requireNonNull(aggregateId, "outbox.aggregate.id.required");
        event.eventType = requireText(eventType, "eventType", EVENT_TYPE_MAX_LENGTH);
        event.payload = requirePayload(payload);
        event.status = OutboxStatus.PENDING;
        event.attemptCount = 0;
        event.availableAt = Objects.requireNonNull(availableAt, "availableAt must not be null");
        event.eventVersion = 1;
        return event;
    }

    public void markProcessing(Instant lockedAt, String lockedBy) {
        this.status = OutboxStatus.PROCESSING;
        this.lockedAt = Objects.requireNonNull(lockedAt, "lockedAt must not be null");
        this.lockedBy = trimToLength(lockedBy, 100);
        this.lastError = null;
    }

    public void markDone(Instant processedAt) {
        this.status = OutboxStatus.DONE;
        this.processedAt = Objects.requireNonNull(processedAt, "processedAt must not be null");
        this.lastError = null;
        this.clearLock();
    }

    public void markFailed(String errorMessage, Instant nextAttemptAt) {
        this.status = OutboxStatus.FAILED;
        this.attemptCount++;
        this.lastError = trimToLength(errorMessage, LAST_ERROR_MAX_LENGTH);
        this.availableAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");
        this.clearLock();
    }

    public void markDead(String errorMessage, Instant deadAt) {
        this.status = OutboxStatus.DEAD;
        this.attemptCount++;
        this.processedAt = Objects.requireNonNull(deadAt, "deadAt must not be null");
        this.lastError = trimToLength(errorMessage, LAST_ERROR_MAX_LENGTH);
        this.clearLock();
    }

    private void clearLock() {
        this.lockedAt = null;
        this.lockedBy = null;
    }

    public void retry(Instant nextAttemptAt) {
        this.status = OutboxStatus.PENDING;
        this.availableAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");
        this.clearLock();
    }

    private static String requirePayload(String payload) {
        Objects.requireNonNull(payload, "outbox.payload.required");
        if (payload.isBlank()) {
            throw new IllegalArgumentException(Translator.toLocale("outbox.payload.blank"));
        }
        return payload;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(Translator.toLocale("validation.field.blank", fieldName));
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(Translator.toLocale("validation.field.max.length", fieldName, maxLength));
        }
        return trimmed;
    }

    private static String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
