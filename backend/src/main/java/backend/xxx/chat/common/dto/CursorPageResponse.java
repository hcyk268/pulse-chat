package backend.xxx.chat.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CursorPageResponse(
        int limit,
        String nextCursor,
        boolean hasMore,
        Instant snapshotAt
) {
}
