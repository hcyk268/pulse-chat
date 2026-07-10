package backend.xxx.chat.outbox.repository;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepositoryCustom {

    List<Long> claimDispatchableEventIds(
            Instant availableAt,
            int limit,
            String lockedBy
    );

    int resetStuckProcessingEvents(
            Instant timeoutBefore,
            Instant now
    );
}
