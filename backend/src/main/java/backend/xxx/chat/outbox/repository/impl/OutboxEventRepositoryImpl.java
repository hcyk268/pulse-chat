package backend.xxx.chat.outbox.repository.impl;

import backend.xxx.chat.outbox.repository.OutboxEventRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
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
        if (isH2()) {
            return claimDispatchableEventIdsForH2(availableAt, limit, lockedBy);
        }

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

        return toLongIds(rows);
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

    private List<Long> claimDispatchableEventIdsForH2(
            Instant availableAt,
            int limit,
            String lockedBy
    ) {
        String selectSql = """
                SELECT id
                FROM outbox_events
                WHERE status IN ('PENDING', 'FAILED')
                  AND available_at <= :availableAt
                ORDER BY created_at ASC, id ASC
                LIMIT :limit
                """;

        List<Long> ids = toLongIds(entityManager.createNativeQuery(selectSql)
                .setParameter("availableAt", availableAt)
                .setParameter("limit", limit)
                .getResultList());

        if (ids.isEmpty()) {
            return ids;
        }

        String updateSql = """
                UPDATE outbox_events
                SET status = 'PROCESSING',
                    locked_at = :lockedAt,
                    locked_by = :lockedBy,
                    last_error = NULL,
                    updated_at = :lockedAt
                WHERE id IN (:ids)
                """;

        entityManager.createNativeQuery(updateSql)
                .setParameter("lockedAt", availableAt)
                .setParameter("lockedBy", lockedBy)
                .setParameter("ids", ids)
                .executeUpdate();

        return ids;
    }

    private boolean isH2() {
        String databaseProductName = entityManager.unwrap(Session.class)
                .doReturningWork(connection -> connection.getMetaData().getDatabaseProductName());
        return "H2".equalsIgnoreCase(databaseProductName);
    }

    private List<Long> toLongIds(List<?> rows) {
        return rows.stream()
                .map(Number.class::cast)
                .map(Number::longValue)
                .toList();
    }
}