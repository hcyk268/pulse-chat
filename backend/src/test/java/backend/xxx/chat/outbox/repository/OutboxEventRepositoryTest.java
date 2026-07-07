package backend.xxx.chat.outbox.repository;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.config.JpaAuditingConfig;
import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.model.OutboxStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findDispatchableEventsReturnsOnlyReadyEventsInCreatedOrder() {
        Instant now = Instant.parse("2026-05-23T12:00:00Z");

        OutboxEvent firstReady = outboxEventRepository.saveAndFlush(
                OutboxEvent.pending("MESSAGE", 10L, "message.created", "{\"messageId\":10}", now.minusSeconds(30))
        );

        OutboxEvent secondReady = outboxEventRepository.saveAndFlush(
                OutboxEvent.pending("MESSAGE", 11L, "message.updated", "{\"messageId\":11}", now.minusSeconds(10))
        );

        OutboxEvent futureEvent = outboxEventRepository.saveAndFlush(
                OutboxEvent.pending("MESSAGE", 12L, "message.deleted", "{\"messageId\":12}", now.plusSeconds(60))
        );

        futureEvent.markFailed("temporary failure", now.plusSeconds(120));
        outboxEventRepository.saveAndFlush(futureEvent);

        entityManager.clear();

        List<OutboxEvent> events = outboxEventRepository.findDispatchableEvents(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                now,
                PageRequest.of(0, 10)
        );

        assertThat(events)
                .extracting(OutboxEvent::getId)
                .containsExactly(firstReady.getId(), secondReady.getId());
        assertThat(events)
                .extracting(OutboxEvent::getStatus)
                .containsExactly(OutboxStatus.PENDING, OutboxStatus.PENDING);
    }
}
