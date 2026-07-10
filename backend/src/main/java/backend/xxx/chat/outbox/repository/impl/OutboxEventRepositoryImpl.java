package backend.xxx.chat.outbox.repository.impl;

import backend.xxx.chat.outbox.repository.OutboxEventRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class OutboxEventRepositoryImpl implements OutboxEventRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Long> claimDispatchableEventIds(
            Instant availableAt,
            int limit,
            String lockedBy
    ) {
        String sql = """
                WITH picked AS (
                    SELECT id
                    FROM outbox_events
                    WHERE status IN ('PENDING', 'FAILED')
                      AND available_at <= :availableAt
                    ORDER BY created_at ASC, id ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE outbox_events e
                SET status = 'PROCESSING',
                    locked_at = :lockedAt,
                    locked_by = :lockedBy,
                    last_error = NULL,
                    updated_at = :lockedAt
                FROM picked
                WHERE e.id = picked.id
                RETURNING e.id
                """;

        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("availableAt", availableAt)
                .setParameter("limit", limit)
                .setParameter("lockedAt", availableAt)
                .setParameter("lockedBy", lockedBy)
                .getResultList();

        return rows.stream()
                .map(Number.class::cast)
                .map(Number::longValue)
                .toList();
    }

    @Override
    public int resetStuckProcessingEvents(
            Instant timeoutBefore,
            Instant now
    ) {
        String sql = """
                UPDATE outbox_events
                SET status = 'FAILED',
                    available_at = :now,
                    locked_at = NULL,
                    locked_by = NULL,
                    last_error = 'Processing timeout; event was reset by outbox worker',
                    updated_at = :now
                WHERE status = 'PROCESSING'
                  AND COALESCE(locked_at, updated_at) < :timeoutBefore
                """;

        return entityManager.createNativeQuery(sql)
                .setParameter("now", now)
                .setParameter("timeoutBefore", timeoutBefore)
                .executeUpdate();
    }
}
