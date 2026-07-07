package backend.xxx.chat.outbox.service;

import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@RequiredArgsConstructor
@Service
public class OutBoxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void pushEvent(String aggregateType, Long aggregateId, String eventType, Object payload) {
        NormalizedInputEvent inputEvent = normalizeInputEvent(aggregateType, aggregateId, eventType, payload);

        OutboxEvent event = OutboxEvent.pending(
                inputEvent.aggregateType(),
                inputEvent.aggregateId(),
                inputEvent.eventType(),
                inputEvent.payload()
        );

        outboxEventRepository.save(event);
    }

    private NormalizedInputEvent normalizeInputEvent(
            String aggregateType,
            Long aggregateId,
            String eventType,
            Object payload
    ) {
        Long normalizedAggregateId = requireAggregateId(aggregateId);
        String normalizedPayload = normalizePayload(payload);

        return new NormalizedInputEvent(
                aggregateType,
                normalizedAggregateId,
                eventType,
                normalizedPayload
        );
    }

    private Long requireAggregateId(Long aggregateId) {
        if (aggregateId == null) {
            throw new IllegalArgumentException("aggregateId must not be null");
        }

        if (aggregateId <= 0) {
            throw new IllegalArgumentException("aggregateId must be positive");
        }

        return aggregateId;
    }

    private String normalizePayload(Object payload) {
        Objects.requireNonNull(payload, "payload must not be null");
        String normalizedPayload = payload instanceof String rawPayload
                ? requireText(rawPayload, "payload")
                : serializeObjectToStringJson(payload);

        try {
            objectMapper.readTree(normalizedPayload);
            return normalizedPayload;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("payload must be valid JSON", ex);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return trimmed;
    }

    private record NormalizedInputEvent(
            String aggregateType,
            Long aggregateId,
            String eventType,
            String payload
    ) {
    }

    private <T> String serializeObjectToStringJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize outbox payload", e);
        }
    }
}
