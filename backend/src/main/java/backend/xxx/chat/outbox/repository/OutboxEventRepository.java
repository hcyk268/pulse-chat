package backend.xxx.chat.outbox.repository;

import java.time.Instant;
import java.util.List;

import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.model.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
            from OutboxEvent outboxEvent
            where outboxEvent.status in :statuses
                and outboxEvent.availableAt <= :availableAt
            order by outboxEvent.createdAt asc, outboxEvent.id asc
            """)
    List<OutboxEvent> findDispatchableEvents(
            @Param("statuses") List<OutboxStatus> statuses,
            @Param("availableAt") Instant availableAt,
            Pageable pageable
    );
}
